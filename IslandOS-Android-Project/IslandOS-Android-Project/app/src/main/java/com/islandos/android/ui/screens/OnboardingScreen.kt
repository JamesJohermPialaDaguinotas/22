package com.islandos.android.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islandos.android.system.PermissionHelper
import com.islandos.android.system.PermissionSnapshot
import com.islandos.android.ui.theme.IslandColors

@Composable
fun OnboardingScreen(
    permissions: PermissionSnapshot,
    onRefresh: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val postNotifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onRefresh() }

    val phoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onRefresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IslandColors.OledBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "IslandOS Setup",
            color = IslandColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "IslandOS requires explicit Android system permissions to render the floating Dynamic Island across apps and display real media and notifications. You can review or revoke any permission at any time.",
            color = IslandColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        PermissionCard(
            title = "1. Display Over Other Apps (Required)",
            description = "Allows the Dynamic Island window (TYPE_APPLICATION_OVERLAY) to float near the front camera across apps and the home screen.",
            granted = permissions.overlayGranted,
            actionLabel = if (permissions.overlayGranted) "Granted" else "Open Overlay Settings",
            onClick = { context.startActivity(PermissionHelper.overlaySettingsIntent(context)) }
        )

        PermissionCard(
            title = "2. Notification & Media Access (Required)",
            description = "Enables NotificationListenerService and MediaSessionManager to read active music tracks, album art, and incoming alerts.",
            granted = permissions.notificationListenerGranted,
            actionLabel = if (permissions.notificationListenerGranted) "Granted" else "Enable Notification Access",
            onClick = { context.startActivity(PermissionHelper.notificationListenerSettingsIntent()) }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionCard(
                title = "3. Foreground Service Notification (Required)",
                description = "Required on Android 13+ so the foreground overlay service stays alive reliably.",
                granted = permissions.postNotificationsGranted,
                actionLabel = if (permissions.postNotificationsGranted) "Granted" else "Allow Notifications",
                onClick = { postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            )
        }

        PermissionCard(
            title = "4. Battery Optimization Exemption (Recommended)",
            description = "Prevents OEM power savers from killing the overlay service when switching apps.",
            granted = permissions.batteryOptimizationIgnored,
            actionLabel = if (permissions.batteryOptimizationIgnored) "Unrestricted" else "Exclude Battery Saver",
            onClick = { context.startActivity(PermissionHelper.batteryOptimizationIntent(context)) }
        )

        PermissionCard(
            title = "5. Call State Visibility (Optional)",
            description = "Shows incoming and active call banners using TelephonyCallback without replacing your default dialer.",
            granted = permissions.phoneStateGranted,
            actionLabel = if (permissions.phoneStateGranted) "Granted" else "Grant Phone State",
            onClick = { phoneLauncher.launch(Manifest.permission.READ_PHONE_STATE) }
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onContinue,
            enabled = permissions.overlayGranted,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = IslandColors.AccentBlue,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (permissions.corePermissionsReady) "Enter IslandOS" else "Continue (Overlay Granted)",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        if (!permissions.overlayGranted) {
            TextButton(onClick = onContinue, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Explore App First (Overlay Disabled)", color = IslandColors.TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(IslandColors.SurfaceDark)
            .border(1.dp, IslandColors.HairlineBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = IslandColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (granted) "Ready" else "Needed",
                color = if (granted) IslandColors.AccentEmerald else IslandColors.AccentAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(text = description, color = IslandColors.TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
        OutlinedButton(
            onClick = onClick,
            enabled = !granted,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(actionLabel, fontSize = 13.sp)
        }
    }
}
