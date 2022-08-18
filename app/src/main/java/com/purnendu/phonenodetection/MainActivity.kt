package com.purnendu.phonenodetection


import android.Manifest
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.*
import com.purnendu.phonenodetection.ui.theme.PhoneNoDetectionTheme



//This Activity will show carrier name and after selection of carrier name it will send sms to desired no
class MainActivity : ComponentActivity() {


    private var no by mutableStateOf("")
    private var isButtonEnable by mutableStateOf(true)
    private var subscriptionId by mutableStateOf<Int?>(null)

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {

            val permissionState = rememberMultiplePermissionsState(
                permissions = listOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.SEND_SMS
                )
            )

            val lifeCycleOwner = LocalLifecycleOwner.current
            DisposableEffect(key1 = lifeCycleOwner)
            {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START) {
                        permissionState.launchMultiplePermissionRequest()
                    }
                }
                lifeCycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifeCycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val context = LocalContext.current
            PhoneNoDetectionTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {


                    val phoneStatePermission = permissionState.permissions[0]

                    if (phoneStatePermission.status.isGranted) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TextField(
                                modifier = Modifier.fillMaxWidth(),
                                placeholder={Text("Enter sender Mobile No")},
                                value = no, onValueChange = { no = it })

                            Spacer(modifier = Modifier.height(10.dp))

                            val subscriptionManager =
                                context.getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

                            val subscriptionInfoList =
                                subscriptionManager.activeSubscriptionInfoList

                            if (subscriptionInfoList.isEmpty()) {
                                Toast.makeText(context, "No Carrier found", Toast.LENGTH_SHORT)
                                    .show()
                                isButtonEnable = false
                            }


                            subscriptionInfoList.forEachIndexed { _, subscriptionInfo ->

                                Row(verticalAlignment = Alignment.CenterVertically) {

                                    RadioButton(
                                        selected = subscriptionId == subscriptionInfo.subscriptionId,
                                        onClick = {
                                            subscriptionId = subscriptionInfo.subscriptionId
                                        })
                                    Text(text = subscriptionInfo.carrierName.toString())
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                            }

                            Button(
                                onClick = {

                                    val sendSmsPermission = permissionState.permissions[1]
                                    if (sendSmsPermission.status.isGranted) {
                                        if (no.isBlank())
                                            return@Button

                                        if (!no.isDigitsOnly())
                                            return@Button

                                        if (no.length != 10)
                                            return@Button

                                        if(subscriptionId==null)
                                            return@Button

                                        val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            context.getSystemService(SmsManager::class.java).createForSubscriptionId(
                                                subscriptionId!!
                                            )
                                        } else {
                                            SmsManager.getDefault()
                                        }

                                        smsManager.sendTextMessage(no, null, "Hello, this is testing", null, null)
                                        Toast.makeText(context, "Message sent", Toast.LENGTH_SHORT).show()



                                    } else if (sendSmsPermission.status.shouldShowRationale) {
                                        Toast.makeText(
                                            context,
                                            "SMS permission is needed to send SMS",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else if (!sendSmsPermission.status.isGranted && !sendSmsPermission.status.shouldShowRationale) {
                                        Toast.makeText(
                                            context,
                                            "SMS permission permanently denied ,you can enable it by going to app setting",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }


                                },
                                enabled = isButtonEnable
                            ) {

                                Text(text = "Send Sms")
                            }

                        }


                    } else if (phoneStatePermission.status.shouldShowRationale) {
                        Snackbar(modifier = Modifier.fillMaxWidth()) {
                            Text(text = "Phone permission is needed to access Carrier ")
                        }
                    } else if (!phoneStatePermission.status.isGranted && !phoneStatePermission.status.shouldShowRationale) {
                        Toast.makeText(
                            context,
                            "Phone permission permanently denied ,you can enable it by going to app setting",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}




