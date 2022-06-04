package mx.edu.ittepic.bluetoothchatapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.os.ResultReceiver
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import mx.edu.ittepic.bluetoothchatapp.databinding.ActivityDeviceListBinding

class DeviceListActivity : AppCompatActivity() {
    lateinit var binding: ActivityDeviceListBinding
    lateinit var bluetoothAdapter: BluetoothAdapter
    var arreglo = ArrayList<String>()
    var arregloAvailabe = ArrayList<String>()
    var arregloDevices = ArrayList<BluetoothDevice>()
    lateinit var progress : ProgressDialog
    var addresses = ArrayList<String>()
    var pairedAddresses = ArrayList<String>()


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setTitle("Conectar a servidor")

        // Lista los dispositivos emparejados
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        var pairedDevices = bluetoothAdapter.bondedDevices
        try {
            if (pairedDevices != null && pairedDevices.size > 0) {
                pairedAddresses.clear()
                pairedDevices.forEach {
                    arreglo.add( it.name + "\n" + it.address )
                    pairedAddresses.add( it.address.toString() )
                }
                binding.listPaired.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arreglo )
            }
        } catch ( err:Exception ) {
            Toast.makeText(this, err.message.toString(), Toast.LENGTH_LONG)
                .show()
        }


        // Registra cuando un dipositivo es detectado
        val filter = IntentFilter( BluetoothDevice.ACTION_FOUND )
        registerReceiver( receiver, filter )
        val filter1 = IntentFilter( BluetoothAdapter.ACTION_DISCOVERY_FINISHED )
        registerReceiver( receiver, filter1 )

        // Selección de la lista
        binding.listAvailable.setOnItemClickListener { adapterView, view, i, l ->
            var addressSelected = addresses.get( i )
            var intent = Intent()
            intent.putExtra("deviceAddress", addressSelected)
            setResult(RESULT_OK, intent)
            finish()
        }
        binding.listPaired.setOnItemClickListener { adapterView, view, i, l ->
            var addressSelected = pairedAddresses.get( i )
            var intent = Intent()
            intent.putExtra("deviceAddress", addressSelected)
            setResult(RESULT_OK, intent)
            finish()
        }

    }

    // Crea el menú
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_device_list, menu)
        return true
    }

    fun actualizaLista() {
        binding.listAvailable.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arregloAvailabe)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when( item.itemId ) {
            R.id.menu_scan_devices -> {
                actualizaLista()
                scanDevices()
                return true
            }
        }
        return true
    }

    // Escanea Dispositivos disponibles
    private fun scanDevices() {
        arregloAvailabe.clear()
        addresses.clear()
        progress = ProgressDialog(this)
        progress.setMessage("Scaning devices...")
        progress.setCancelable(false)
        progress.show()

        if ( bluetoothAdapter.isDiscovering ) {
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()

    }


    // Crear receptor
    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if ( BluetoothDevice.ACTION_FOUND.equals( action ) ) {
                var device : BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    if ( device.bondState != BluetoothDevice.BOND_BONDED ) {
                        arregloAvailabe.add( device.name + "\n" + device.address )
                        arregloDevices.add( device )
                        addresses.add( device.address.toString() )
                        actualizaLista()
                    }
                }
            } else if ( BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action) ) {
                if ( progress.isShowing ) {
                    progress.dismiss()
                }
                    actualizaLista()
                if ( arregloAvailabe.count() == 0 ) {
                    Toast.makeText(context, "No new devices found", Toast.LENGTH_LONG)
                        .show()
                } else {
                    Toast.makeText(context, "Click on the device to start a chat", Toast.LENGTH_LONG)
                        .show()
                }
            }


        }

    }
}