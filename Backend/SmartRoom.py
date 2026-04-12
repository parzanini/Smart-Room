# app.py
from flask import Flask, jsonify, request  # Import Flask for web server, jsonify for JSON responses, request for handling HTTP requests
from sense_hat import SenseHat  # Import SenseHat library to interact with Raspberry Pi Sense HAT
import pymysql  # Import pymysql to connect to the MySQL database
from pymysql.cursors import DictCursor  # Import DictCursor so database results are returned as dictionaries
import threading  # Import threading to run background tasks
import time  # Import time for sleep delays
import os  # Import os to read environment variables

app = Flask(__name__)  # Initialize the Flask application
sense = SenseHat()  # Initialize the SenseHat hardware interface

# Read database connection settings from environment variables with defaults
DB_HOST = os.getenv("DB_HOST", "localhost")  # Database host address
DB_USER = os.getenv("DB_USER", "admin")  # Database username
DB_PASSWORD = os.getenv("DB_PASSWORD", "admin")  # Database password
DB_NAME = os.getenv("DB_NAME", "smart_room")  # Database name
CAPTURE_INTERVAL = int(os.getenv("CAPTURE_INTERVAL", "60"))  # Data capture interval in seconds

def get_connection():
    # Helper function to establish and return a new database connection
    return pymysql.connect(
        host=DB_HOST,  # Use the DB host from configuration
        user=DB_USER,  # Use the DB user from configuration
        password=DB_PASSWORD,  # Use the DB password from configuration
        database=DB_NAME,  # Use the DB name from configuration
        cursorclass=DictCursor,  # Ensure results come back as key-value dictionaries
        autocommit=True  # Automatically commit transactions to the database
    )

def read_sensor_data():
    # Read the current temperature from the Sense HAT and round to 2 decimals
    temperature = round(float(sense.get_temperature()), 2)
    # Read the current humidity from the Sense HAT and round to 2 decimals
    humidity = round(float(sense.get_humidity()), 2)
    # Return both temperature and humidity
    return temperature, humidity

def save_reading():
    # Get the latest temperature and humidity from the sensors
    temperature, humidity = read_sensor_data()

    # Get a new database connection
    conn = get_connection()
    try:
        # Create a database cursor to execute SQL commands
        with conn.cursor() as cur:
            cur.execute(
                "INSERT INTO sensor_readings (temperature, humidity) VALUES (%s, %s)",
                (temperature, humidity)
            )
            reading_id = cur.lastrowid

        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT id, temperature, humidity,
                       DATE_FORMAT(created_at, '%%Y-%%m-%%dT%%H:%%i:%%sZ') AS timestamp
                FROM sensor_readings
                WHERE id = %s
                """,
                (reading_id,)  # Filter by the newly inserted ID
            )
            # Return the single row fetched from the database
            return cur.fetchone()
    finally:
        # Ensure the database connection is closed safely
        conn.close()

# 1. CURRENT DATA (Used by the Android Dashboard polling)
@app.route("/api/current", methods=["GET"])  # Define a GET route for fetching the current reading
def current_reading():
    # Get a new database connection
    conn = get_connection()
    try:
        # Create a cursor for database operations
        with conn.cursor() as cur:
            # Execute a SELECT query to get the most recent reading
            cur.execute(
                """
                SELECT temperature, humidity,
                       DATE_FORMAT(created_at, '%%Y-%%m-%%dT%%H:%%i:%%sZ') AS timestamp
                FROM sensor_readings
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """
            )
            # Fetch the requested single row
            row = cur.fetchone()
            # Return the row as a JSON response, or empty JSON if no row exists
            return jsonify(row if row else {})
    finally:
        # Close the DB connection
        conn.close()

# 2. HISTORICAL DATA (Used by the Android History screen)
@app.route("/api/data", methods=["GET"])  # Define a GET route for fetching historical data
def get_data():
    # Read optional start timestamp from the query parameters
    start = request.args.get("start")
    # Read optional end timestamp from the query parameters
    end = request.args.get("end")
    limit = int(request.args.get("limit", 1000)) # Default to 1000 for better history

    conn = get_connection()
    try:
        # Open cursor
        with conn.cursor() as cur:
            # Base query to fetch all readings and format timestamps
            query = """
                SELECT temperature, humidity,
                       DATE_FORMAT(created_at, '%%Y-%%m-%%dT%%H:%%i:%%sZ') AS timestamp
                FROM sensor_readings
            """
            # List to hold query string parameters
            params = []
            
            # If both start and end timestamps are provided by the client
            if start and end:
                # Convert the ISO format (YYYY-MM-DDTHH:MM:SSZ) back to MySQL format (YYYY-MM-DD HH:MM:SS) for querying
                start_str = start.replace("T", " ").replace("Z", "")
                end_str = end.replace("T", " ").replace("Z", "")
                # Append WHERE clause to filter between start and end dates
                query += " WHERE created_at >= %s AND created_at <= %s "
                # Add the parsed strings to parameters list
                params.extend([start_str, end_str])
                
            query += f" ORDER BY created_at DESC, id DESC LIMIT {limit}"
            
            # Execute the constructed query with the parameters safely bound
            cur.execute(query, tuple(params))
            # Fetch all matching rows
            rows = cur.fetchall()

            # If too many points, sample them (e.g., 100 points max for the UI)
            # This keeps the chart clean regardless of the timeframe.
            if len(rows) > 200:
                step = len(rows) // 100
                rows = rows[::step]

            return jsonify(rows)
    finally:
        # Close connection
        conn.close()

# 3. ACTUATOR CONTROL (Used by the Android Fan Toggle button)
@app.route("/api/actuator", methods=["POST"])  # Define a POST route for remote control
def manage_actuator():
    try:
        # Parse the incoming JSON body
        data = request.get_json() or {}
        device = data.get("device", "unknown")

        # Explicitly handle state as boolean, even if it comes as string or int
        state_raw = data.get("state", False)
        if isinstance(state_raw, str):
            state = state_raw.lower() in ["true", "1", "on"]
        else:
            state = bool(state_raw)

        # Determine message and color
        state_txt = "ON" if state else "OFF"
        # Colors: Blue (0, 0, 255) for ON, Red (255, 0, 0) for OFF
        color = (0, 0, 255) if state else (255, 0, 0)

        print(f"DEBUG: Actuator '{device}' toggle. New state: {state_txt}")

        # 1. Scroll the text "ON" or "OFF"
        # We use white text on the target background color for visibility
        sense.show_message(state_txt, text_colour=(255, 255, 255), back_colour=(0, 0, 0), scroll_speed=0.05)

        # 2. Fill the whole screen with the state color (Blue or Red)
        sense.clear(color)

        return jsonify({
            "status": "success",
            "message": f"{device.capitalize()} turned {state_txt.lower()}"
        })
    except Exception as e:
        print(f"ERROR in manage_actuator: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

def background_capture():
    # Infinite loop to keep capturing data continuously
    while True:
        try:
            # Call save_reading to insert current sensor data into the DB
            save_reading()
        except Exception as e:
            # Print any errors encountered during capture
            print(f"Capture error: {e}")
        # Pause execution for the defined capture interval before taking next reading
        time.sleep(CAPTURE_INTERVAL)

if __name__ == "__main__":
    # Start the background capture task in a separate daemon thread
    threading.Thread(target=background_capture, daemon=True).start()
    # Start the Flask web server on all network interfaces
    app.run(host="0.0.0.0", port=5000, debug=False)

