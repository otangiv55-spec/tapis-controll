package com.example.tapiscontrol

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Apre l'app musicale predefinita del telefono (Spotify, YouTube Music...). */
private fun openMusicApp(ctx: Context) {
    try {
        val i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    } catch (_: Exception) {
        Toast.makeText(ctx, "Nessuna app musicale trovata", Toast.LENGTH_SHORT).show()
    }
}

/** Musica da file del telefono, accesso rapido a un'app musicale e interruttore per l'audio del video. */
@Composable
fun MusicCard(vm: TapisViewModel) {
    val ctx = LocalContext.current
    val list by vm.music.playlist.collectAsStateWithLifecycle()
    val playing by vm.music.playing.collectAsStateWithLifecycle()
    val volume by vm.music.volume.collectAsStateWithLifecycle()
    val current by vm.music.current.collectAsStateWithLifecycle()
    val muted by vm.videoMuted.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach {
                try {
                    ctx.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {
                }
            }
            vm.music.setPlaylist(uris.map { it.toString() })
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Musica", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }) { Text("Scegli brani") }
                OutlinedButton(onClick = { openMusicApp(ctx) }) { Text("Apri app musicale") }
            }
            if (list.isNotEmpty()) {
                Text(current)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { vm.music.previous() }) { Text("⏮") }
                    Button(onClick = { vm.music.toggle() }) { Text(if (playing) "⏸ Pausa" else "▶ Suona") }
                    OutlinedButton(onClick = { vm.music.next() }) { Text("⏭") }
                    TextButton(onClick = { vm.music.clear() }) { Text("Svuota") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Volume")
                    Slider(
                        value = volume,
                        onValueChange = { vm.music.setVolume(it) },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = muted, onCheckedChange = { vm.setVideoMuted(it) })
                Text("Silenzia l'audio del video")
            }
            Text(
                "Per usare un'app come Spotify: avviala, fai partire la musica e torna qui; l'audio continua " +
                    "insieme al video e agli avvisi. Silenzia il video per sentire solo la musica.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Interruttore "mantieni costanti" (blocca le variazioni di velocita' e pendenza dell'allenamento). */
@Composable
fun HoldSwitch(vm: TapisViewModel) {
    val hold by vm.holdConstant.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = hold, onCheckedChange = { vm.setHold(it) })
            Text("Mantieni costanti velocità e pendenza", style = MaterialTheme.typography.titleSmall)
        }
        if (hold) {
            Text(
                "Le variazioni previste dall'allenamento (o dal profilo del video) sono sospese: restano i valori " +
                    "attuali, che puoi comunque correggere a mano. Spegni per riprendere con una transizione graduale.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
