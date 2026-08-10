package com.example.smarthomesystem

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ServerValue

object FirebaseRepository {
    private const val DATABASE_URL = "https://smarthomesystem-70abf-default-rtdb.firebaseio.com"
    private val database = FirebaseDatabase.getInstance(DATABASE_URL)

    fun listenToFloors(onDataChanged: (List<Floor>) -> Unit) {
        database.getReference("floors").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Ensure strictly Ground floor and First floor are returned
                val validFloors = listOf(
                    Floor(id = "ground", name = "Ground floor"),
                    Floor(id = "first", name = "First floor")
                )
                onDataChanged(validFloors)
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

    fun getCachedDevice(floorId: String, deviceId: String): Device? {
        return null
    }

    fun updateDeviceState(floorId: String, deviceId: String, newState: String) {
        val ref = database.getReference("floors/$floorId/devices/$deviceId")
        val updates = mutableMapOf<String, Any>("state" to newState)
        if (newState == "ON") {
            updates["turnedOnAt"] = ServerValue.TIMESTAMP
        } else {
            updates["turnedOnAt"] = 0L
        }
        ref.updateChildren(updates)
    }

    fun updateSchedule(floorId: String, deviceId: String, type: String, time: String) {
        val field = if (type == "ON") "scheduleOn" else "scheduleOff"
        database.getReference("floors/$floorId/devices/$deviceId/$field").setValue(time)
    }

    fun updateMaxDuration(floorId: String, deviceId: String, durationSec: Int, resetTimer: Boolean = false) {
        val ref = database.getReference("floors/$floorId/devices/$deviceId")
        val updates = mutableMapOf<String, Any>("maxDurationSec" to durationSec)
        if (resetTimer) {
            updates["turnedOnAt"] = ServerValue.TIMESTAMP
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

    fun listenToServerTimeOffset(onOffsetChanged: (Long) -> Unit) {
        database.getReference(".info/serverTimeOffset").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onOffsetChanged(snapshot.getValue(Long::class.java) ?: 0L)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun addSampleData() {
        val floorsRef = database.getReference("floors")
        
        // Remove all previous duplicate/empty floor entries from Firebase RTDB
        floorsRef.removeValue()
        
        // 1. Ground floor (Kitchen, Living room, Garage, Porch, Utility)
        val groundId = "ground"
        floorsRef.child(groundId).child("name").setValue("Ground floor")
        val groundDevices = floorsRef.child(groundId).child("devices")
        
        // Kitchen
        groundDevices.child("outlet1").setValue(Device(name = "Outlet 1", type = "outlet", state = "ON", room = "Kitchen"))
        groundDevices.child("light1").setValue(Device(name = "Light 1", type = "light", state = "ON", room = "Kitchen"))
        groundDevices.child("toaster1").setValue(Device(name = "Toaster plug", type = "outlet", state = "OFF", room = "Kitchen"))
        
        // Living room
        groundDevices.child("gang1").setValue(Device(name = "Gang switch unit", type = "multiswitch", state = "OFF", room = "Living room", switches = mapOf("sw1_lamp" to "ON", "sw2_fan" to "OFF", "sw3_TV plug" to "ON")))
        groundDevices.child("lamp1").setValue(Device(name = "Floor lamp", type = "light", state = "ON", room = "Living room"))
        
        // Garage (Main Garage Door & Light)
        groundDevices.child("garagedoor1").setValue(Device(name = "Main Garage Door", type = "outlet", state = "OFF", room = "Garage"))
        groundDevices.child("garagelight1").setValue(Device(name = "Garage overhead light", type = "light", state = "ON", room = "Garage"))

        // Porch
        groundDevices.child("cam1").setValue(Device(name = "Front door camera", type = "camera", state = "STREAMING", room = "Porch", streamUrl = "rtsp://mock-stream/porch"))

        // Utility (Clothing iron)
        val eightMinLeftTimestamp = System.currentTimeMillis() - ((900 - 480) * 1000L)
        groundDevices.child("iron1").setValue(Device(name = "Clothing iron", type = "iron", state = "ON", room = "Utility", maxDurationSec = 900, turnedOnAt = eightMinLeftTimestamp))

        // 2. First floor (Master Bedroom, Bedroom 2, Bathroom, Balcony)
        val firstId = "first"
        floorsRef.child(firstId).child("name").setValue("First floor")
        val firstDevices = floorsRef.child(firstId).child("devices")

        // Master Bedroom
        firstDevices.child("bedlamp1").setValue(Device(name = "Bedside lamp", type = "light", state = "ON", room = "Master Bedroom", scheduleOn = "18:00", scheduleOff = "06:00"))
        firstDevices.child("bedroomac").setValue(Device(name = "Bedroom AC plug", type = "outlet", state = "ON", room = "Master Bedroom"))

        // Bedroom 2
        firstDevices.child("fan1").setValue(Device(name = "Ceiling fan", type = "outlet", state = "ON", room = "Bedroom 2"))
        firstDevices.child("desklamp1").setValue(Device(name = "Study desk lamp", type = "light", state = "OFF", room = "Bedroom 2", scheduleOn = "19:00", scheduleOff = "23:00"))

        // Bathroom
        firstDevices.child("vanitylight1").setValue(Device(name = "Vanity mirror light", type = "light", state = "ON", room = "Bathroom"))
        firstDevices.child("exhaust1").setValue(Device(name = "Exhaust fan", type = "outlet", state = "OFF", room = "Bathroom"))

        // Balcony
        firstDevices.child("balconylight1").setValue(Device(name = "Balcony light", type = "light", state = "OFF", room = "Balcony"))
        firstDevices.child("balconycam1").setValue(Device(name = "Balcony camera", type = "camera", state = "STREAMING", room = "Balcony"))
    }
}
