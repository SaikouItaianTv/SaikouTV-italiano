package ani.saikou.tv.login

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import ani.saikou.anilist.Anilist
import ani.saikou.databinding.TvLoginFragmentBinding
import ani.saikou.databinding.TvNearbyLoginFragmentBinding
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener


class TVNearbyLoginFragment() : Fragment() {

    val connectionCallback = ConnectionCallback()
    lateinit var binding: TvNearbyLoginFragmentBinding

    val finePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        when {
            granted -> {
                    startDiscovery()
            }
            !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                binding.progress.visibility = View.INVISIBLE
                binding.text.visibility = View.VISIBLE
                binding.text.text = "Permission denied"
            }
            else -> {
                binding.progress.visibility = View.INVISIBLE
                binding.text.visibility = View.VISIBLE
                binding.text.text = "Permission denied"
            }
        }
    }

    val coarsePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        when {
            granted -> {

                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startDiscovery()
                } else {
                    finePermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                binding.progress.visibility = View.INVISIBLE
                binding.text.visibility = View.VISIBLE
                binding.text.text = "Permission denied"
            }
            else -> {
                binding.progress.visibility = View.INVISIBLE
                binding.text.visibility = View.VISIBLE
                binding.text.text = "Permission denied"
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = TvNearbyLoginFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        coarsePermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun startDiscovery() {
        binding.text.text = "Initializing...\nThis could take some time\nPlease make sure you have Saikou installed on your phone and logged in"
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        Nearby.getConnectionsClient(requireContext())
            .startDiscovery(NearbyTVConnection.SERVICE_ID, object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(p0: String, p1: DiscoveredEndpointInfo) {
                    if(p1.serviceId == NearbyTVConnection.SERVICE_ID && p1.endpointName == NearbyTVConnection.PHONE_NAME) {
                        binding.text.text = "Connecting to " + p1.endpointName + "..."
                        Nearby.getConnectionsClient(context!!)
                            .requestConnection(
                                NearbyTVConnection.TV_NAME,
                                p0,
                                connectionCallback
                            )
                            .addOnSuccessListener(
                                OnSuccessListener { unused: Void? -> })
                            .addOnFailureListener(
                                OnFailureListener { e: Exception? ->
                                    binding.text.text = "Please open Saikou on your phone and login..."})
                    }
                }

                override fun onEndpointLost(p0: String) {

                }
            }, discoveryOptions)
            .addOnSuccessListener {
                binding.progress.visibility = View.INVISIBLE
                binding.text.text = "Please open Saikou on your phone and login"
            }
            .addOnFailureListener { e: java.lang.Exception? ->
                binding.progress.visibility = View.INVISIBLE
                binding.text.text = e?.message ?: "Error"
            }
    }

    inner class ConnectionCallback: ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
            binding.text.text = p1.authenticationDigits
            Nearby.getConnectionsClient(requireContext())
                .acceptConnection(p0, object : PayloadCallback() {
                    override fun onPayloadReceived(p0: String, p1: Payload) {
                        if (p1.getType() === Payload.Type.BYTES) {
                            p1?.asBytes()?.let {
                                val token = String(it)
                                saveToken(token)
                                Nearby.getConnectionsClient(requireContext()).disconnectFromEndpoint(p0)
                                //TVAnimeFragment.shouldReload = true
                                requireActivity().supportFragmentManager.popBackStack()
                            } ?: run {
                                binding.text.text = "Something went wrong while processing your login info"
                            }
                        }
                    }
                    override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {}
                })
        }

        override fun onConnectionResult(
            p0: String,
            p1: ConnectionResolution
        ) {
            when (p1.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Nearby.getConnectionsClient(requireContext()).stopAdvertising()
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    binding.text.text = "You need to accept the TV connection popup on your phone"
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    binding.text.text = "Something went wrong, trying again..."
                    binding.progress.visibility = View.VISIBLE
                }
                else -> {
                }
            }
        }

        override fun onDisconnected(p0: String) {}
    }

    private fun saveToken(token: String) {
        Anilist.token = token
        val filename = "anilistToken"
        requireActivity().openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(token.toByteArray())
        }
    }

    override fun onDestroy() {
        Nearby.getConnectionsClient(requireContext()).stopDiscovery()
        super.onDestroy()
    }
}