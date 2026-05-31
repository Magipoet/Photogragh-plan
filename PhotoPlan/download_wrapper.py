import urllib.request
import os

url = "https://services.gradle.org/distributions/gradle-8.9-wrapper.jar"
target_path = "/remote-home/share/lijl/task_all/14_Photogragh-plan/PhotoPlan/gradle/wrapper/gradle-wrapper.jar"

os.makedirs(os.path.dirname(target_path), exist_ok=True)

print(f"Downloading gradle-wrapper.jar from {url}...")
try:
    urllib.request.urlretrieve(url, target_path)
    print(f"Successfully downloaded to {target_path}")
    print(f"File size: {os.path.getsize(target_path)} bytes")
except Exception as e:
    print(f"Error downloading: {e}")
