# app.py
from flask import Flask, jsonify, request
from sense_hat import SenseHat
import pymysql
from pymysql.cursors import DictCursor
import threading
import time
import os

app = Flask(__name__)
sense = SenseHat()

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_USER = os.getenv("DB_USER", "admin")
DB_PASSWORD = os.getenv("DB_PASSWORD", "admin")
DB_NAME = os.getenv("DB_NAME", "smart_room")
CAPTURE_INTERVAL = int(os.getenv("CAPTURE_INTERVAL", "60"))

def get_connection():
    return pymysql.connect(
        host=DB_HOST,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME,
        cursorclass=DictCursor,
        autocommit=True
    )

def read_sensor_data():
    temperature = round(float(sense.get_temperature()), 2)
    humidity = round(float(sense.get_humidity()), 2)
    return temperature, humidity

def save_reading():
    temperature, humidity = read_sensor_data()

    conn = get_connection()
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO sensor_readings (temperature, humidity)
                VALUES (%s, %s)
                """,
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
                (reading_id,)
            )
            return cur.fetchone()
    finally:
        conn.close()

# 1. CURRENT DATA (Used by the Android Dashboard polling)
@app.route("/api/current", methods=["GET"])
def current_reading():
    conn = get_connection()
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT temperature, humidity,
                       DATE_FORMAT(created_at, '%%Y-%%m-%%dT%%H:%%i:%%sZ') AS timestamp
                FROM sensor_readings
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """
            )
            row = cur.fetchone()
            return jsonify(row if row else {})
    finally:
        conn.close()

# 2. HISTORICAL DATA (Used by the Android History screen)
@app.route("/api/data", methods=["GET"])
def get_data():
    start = request.args.get("start")
    end = request.args.get("end")
    
    conn = get_connection()
    try:
        with conn.cursor() as cur:
            query = """
                SELECT temperature, humidity,
                       DATE_FORMAT(created_at, '%%Y-%%m-%%dT%%H:%%i:%%sZ') AS timestamp
                FROM sensor_readings
            """
            params = []
            
            if start and end:
                # Convert the ISO format (YYYY-MM-DDTHH:MM:SSZ) back to MySQL format (YYYY-MM-DD HH:MM:SS) for querying
                start_str = start.replace("T", " ").replace("Z", "")
                end_str = end.replace("T", " ").replace("Z", "")
                query += " WHERE created_at >= %s AND created_at <= %s "
                params.extend([start_str, end_str])
                
            query += " ORDER BY created_at DESC, id DESC LIMIT 100"
            
            cur.execute(query, tuple(params))
            rows = cur.fetchall()
            return jsonify(rows)
    finally:
        conn.close()

# 3. ACTUATOR CONTROL (Used by the Android Fan Toggle button)
@app.route("/api/actuator", methods=["POST"])
def manage_actuator():
    data = request.get_json() or {}
    device = data.get("device", "unknown")
    state = data.get("state", False)
    
    state_str = "on" if state else "off"
    
    # Simulate turning the fan/heater on or off 
    print(f"*** ACTUATOR TRIGGERED: {device} turned {state_str.upper()} ***")
    
    return jsonify({
        "status": "success",
        "message": f"{device.capitalize()} turned {state_str}"
    })

def background_capture():
    while True:
        try:
            save_reading()
        except Exception as e:
            print(f"Capture error: {e}")
        time.sleep(CAPTURE_INTERVAL)

if __name__ == "__main__":
    threading.Thread(target=background_capture, daemon=True).start()
    app.run(host="0.0.0.0", port=5000, debug=False)

