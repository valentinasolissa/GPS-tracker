import base64
import tracker_pb2
from supabase import create_client
import os
from dotenv import load_dotenv

load_dotenv()
supabase = create_client(os.getenv("SUPABASE_URL"), os.getenv("SUPABASE_KEY"))

# 1. Pull the latest row from Supabase
response = supabase.table("locations").select("raw_payload").order("id", desc=True).limit(1).execute()
cloud_string = response.data[0]['raw_payload']

print(f"Cloud String: {cloud_string}")

try:
    # 2. Try to decode it
    # If it was sent as Base64, we decode it first
    binary_data = base64.b64decode(cloud_string)
    
    # 3. Parse with Protobuf
    status = tracker_pb2.DeviceStatus()
    status.ParseFromString(binary_data)
    
    print("--- SUCCESS ---")
    print(f"Device: {status.device_id}")
    print(f"Lat/Long: {status.latitude}, {status.longitude}")
except Exception as e:
    print(f"--- FAILED TO DECODE ---")
    print(f"Error: {e}")
