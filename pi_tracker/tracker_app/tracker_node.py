import os
import time
import base64
from dotenv import load_dotenv
from supabase import create_client, Client
import tracker_pb2
import requests

load_dotenv()
supabase: Client = create_client(os.getenv("SUPABASE_URL"), os.getenv("SUPABASE_KEY"))

def fetch_location():
    """Simulates a GPS sensor using the IP Geolocation API."""
    try:
        # Free endpoint: no key required (limited to 45 requests/min)
        response = requests.get("http://ip-api.com/json/")
        location = response.json()
        
        if location.get("status") == "success":
            print(f"Success! Lat: {location["lat"]}, Lon: {location["lon"]}") 
            return location["lat"], location["lon"]
        else:
            print(f"Location error: {location.get('message')}")
            return None, None
    except Exception as e:
        print(f"Network error: {e}")
        return None, None

def send_pure_protobuf():
    # 1. Initialize the Protobuf Object
    lat, lon = fetch_location()

    status = tracker_pb2.DeviceStatus()
    status.device_id = "VALENTINA-PI-4B"
    status.latitude = lat
    status.longitude = lon
    status.is_safe = True
    status.timestamp = int(time.time())

    # 2. THE CORE STEP: Serialize to Binary
    # This turns the object into a compact 'bytes' object.
    binary_blob = status.SerializeToString()
    
    # 3. Base64 Encoding
    # We do this because the Supabase API expects text-safe characters.
    encoded_blob = base64.b64encode(binary_blob).decode('utf-8')

    # 4. Push to Cloud
    # We only send the ID (for filtering) and the binary blob.
    data = {
        "device_id": status.device_id,
        "raw_payload": encoded_blob 
    }

    try:
        supabase.table("locations").insert(data).execute()
        print(f"Binary Payload Sent! Size: {len(binary_blob)} bytes")
        print(f"Original Base64: {encoded_blob}")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    send_pure_protobuf()
