package mx.edu.ittepic.bluetoothchatapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import mx.edu.ittepic.bluetoothchatapp.databinding.ActivityMainBinding
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var mState: Int = 0
    private var mNewState: Int = 0

    val permition_request = 101
    val select_device = 102

    // UUID utilizada
    var m_myUUID : UUID = UUID.fromString("bdbb1465-a247-4c66-8759-557365bd77cc")

    // Dirección obtenida
    lateinit var m_address : String

    // Constantes que indican la situación de la conexión
    companion object {
        val STATE_NONE = 0       // we're doing nothing
        val STATE_LISTEN = 1     // now listening for incoming connections
        val STATE_CONNECTING = 2 // now initiating an outgoing connection
        val STATE_CONNECTED = 3  // now connected to a remote device
    }

    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if ( bluetoothAdapter == null ) {
            Toast.makeText(this, "No Bluetooth Found", Toast.LENGTH_LONG)
                .show()
        }
        mState = STATE_NONE
        mNewState = mState
        //mHandler = handler
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView( binding.root )

        setTitle("Blue Client")

        // Imagen inicial
        binding.imagen.setImageResource(R.drawable.ic_baseline_bluetooth_disabled_24)

        if ( bluetoothAdapter!!.isEnabled ) {
            binding.imagen.setImageResource(R.drawable.ic_baseline_bluetooth)
        }


    }

    // Vincular el menú
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        return true
    }

    // Selección del item menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when ( item.itemId ) {
            R.id.menu_search_devices -> {
                checkPermitions()
                return true
            }

            R.id.menu_enable_bluetooth -> {
                enableBluetooth()
                return true
            }

            R.id.menu_disconnect -> {
                //disconnect()
                AcceptThread().cancel()
                return true
            }
        }
        return true
    }

    // Activa el bluetooth
    @SuppressLint("MissingPermission")
    private fun enableBluetooth() {
        if ( !bluetoothAdapter!!.isEnabled!!) {
            bluetoothAdapter!!.enable()
            binding.imagen.setImageResource(R.drawable.ic_baseline_bluetooth)
        }

        // Activa la visibilidad
        if ( bluetoothAdapter!!.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE ) {
            var discoveryIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(discoveryIntent)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if ( requestCode == permition_request ) {
            if ( grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED &&
                grantResults[3] == PackageManager.PERMISSION_GRANTED && grantResults[4] == PackageManager.PERMISSION_GRANTED) {
                var otraVentana = Intent(this, DeviceListActivity::class.java)
                startActivityForResult(otraVentana, select_device)
            } else {
                AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage("Location permition is required\nPlease grant")
                    .setPositiveButton("Grant") { d, i ->
                        checkPermitions()
                    }
                    .setNegativeButton("Deny") { d, i ->
                        finish()
                    }
                    .show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }

    }

    // Se obtiene el Address de la activity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ( requestCode == select_device && resultCode == RESULT_OK ) {
            var address = data?.getStringExtra("deviceAddress").toString()
            if (address != null) {
                m_address = address
                val device : BluetoothDevice = bluetoothAdapter!!.getRemoteDevice( m_address )
                Toast.makeText(this, "Address: ${address}", Toast.LENGTH_SHORT)
                    .show()
                ConnectThread( device ).start()
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    // Analiza permisos
    private fun checkPermitions() {
        if ( ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, arrayOf( android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.BLUETOOTH_ADMIN, android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT), permition_request )
        }else {
            var otraVentana = Intent(this, DeviceListActivity::class.java)
            startActivityForResult(otraVentana, select_device)
        }
    }


    // Conectar 3 supongo
    @SuppressLint("MissingPermission")
    private inner class ConnectThread(device:BluetoothDevice ) : Thread() {
        private val mmSocket : BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord( m_myUUID )
        }

        @SuppressLint("MissingPermission")
        override fun run() {
            super.run()
            if ( bluetoothAdapter!!.isDiscovering ) {
                bluetoothAdapter!!.cancelDiscovery()
            }

            mmSocket?.let { socket ->
                socket.connect()

                //manageMyConnectedSocked(socket)
            }
        }
        fun cancel() {
            try {
                mmSocket?.close()
            } catch ( err:IOException ) {
                Log.e("Conection", "Could not close the client socket", err)
            }
        }
    }


    // Server :/
    @SuppressLint("MissingPermission")
    private inner class AcceptThread() : Thread() {
        private val mmServerSocket : BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter!!.listenUsingRfcommWithServiceRecord("Servidor", m_myUUID )
        }

        override fun run() {
            super.run()
            var shouldLoop = true
            while ( shouldLoop ) {
                val socket : BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch ( err:IOException ) {
                    Log.e("Server","Socket Accept() nethod failed", err )
                    shouldLoop = false
                    null
                }
                socket?.also {
                    //manageMyConnectedSocket(it)
                    setTitle("Conectado")
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch ( err:IOException ) {
                Log.e("Server","Could not close the connect socket")
            }
        }
    }
}