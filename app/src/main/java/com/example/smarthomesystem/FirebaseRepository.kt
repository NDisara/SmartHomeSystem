package com.example.smarthomesystem

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object FirebaseRepository {
    private const val DATABASE_URL = "https://smarthomesystem-70abf-default-rtdb.firebaseio.com"
    private val database = FirebaseDatabase.getInstance(DATABASE_URL)

    fun listenToFloors(onDataChanged: (List<Floor>) -> Unit) {
        database.getReference("floors").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val floors = mutableListOf<Floor>()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: id
                    floors.add(Floor(id = id, name = name))
                }
                onDataChanged(floors)
            }
            override fun onCancelled(error: DatabaseError) {
                println("Firebase Error: ${error.message}")
            }
        })
    }

    fun listenToDevices(floorId: String, onDataChanged: (List<Device>) -> Unit) {
        database.getReference("floors/$floorId/devices").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val devices = mutableListOf<Device>()
                for (child in snapshot.children) {
                    val device = child.getValue(Device::class.java)?.copy(id = child.key ?: "", floorId = floorId)
                    if (device != null) devices.add(device)
                }
                onDataChanged(devices)
            }
            override fun onCancelled(error: DatabaseError) {
                println("Firebase Error: ${error.message}")
            }
        })
    }
    fun updateDeviceState(floorId: String, deviceId: String, newState: String, turnedOnAt: Long = 0L) {
        val ref = database.getReference("floors/$floorId/devices/$deviceId")
        val updates = mutableMapOf<String, Any>("state" to newState)
        if (newState == "ON") {
            updates["turnedOnAt"] = turnedOnAt
        } else {
            updates["turnedOnAt"] = 0L
        }
        ref.updateChildren(updates)
    }

    fun updateSwitchState(floorId: String, deviceId: String, switchId: String, newState: String) {
        database.getReference("floors/$floorId/devices/$deviceId/switches/$switchId").setValue(newState)
    }

    fun logUsage(deviceId: String, deviceName: String, action: String) {
        val log = UsageLog(deviceId, deviceName, action, System.currentTimeMillis())
        database.getReference("usageLogs").push().setValue(log)
    }

    fun listenToLogs(onDataChanged: (List<UsageLog>) -> Unit) {
        database.getReference("usageLogs").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val logs = mutableListOf<UsageLog>()
                for (child in snapshot.children) {
                    child.getValue(UsageLog::class.java)?.let { logs.add(it) }
                }
                onDataChanged(logs)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun addSampleData() {
        val floorsRef = database.getReference("floors")
        
        // Ground Floor
        val groundId = "ground"
        floorsRef.child(groundId).child("name").setValue("Ground floor")
        val groundDevices = floorsRef.child(groundId).child("devices")
        groundDevices.child("outlet1").setValue(Device(name = "Kitchen outlet", type = "outlet", state = "ON"))
        groundDevices.child("iron1").setValue(Device(name = "Utility iron", type = "iron", state = "OFF", maxDurationSec = 900))
        groundDevices.child("cam1").setValue(Device(name = "Porch camera", type = "camera", streamUrl = "rtsp://mock-stream/porch"))
        groundDevices.child("light1").setValue(Device(name = "Hallway light", type = "light", state = "OFF", scheduleOn = "18:00", scheduleOff = "06:00"))

        // First Floor
        val firstId = "first"
        floorsRef.child(firstId).child("name").setValue("First floor")
        val firstDevices = floorsRef.child(firstId).child("devices")
        firstDevices.child("gangbox1").setValue(mapOf(
            "name" to "Living room gang-box",
            "type" to "multiswitch",
            "switches" to mapOf("sw1_lamp" to "ON", "sw2_fan" to "OFF", "sw3_TV plug" to "ON")
        ))
        
        // Garage
        val garageId = "garage"
        floorsRef.child(garageId).child("name").setValue("Garage")
        val garageDevices = floorsRef.child(garageId).child("devices")
        garageDevices.child("door1").setValue(Device(name = "Main Garage Door", type = "outlet", state = "OFF"))
    }
}
