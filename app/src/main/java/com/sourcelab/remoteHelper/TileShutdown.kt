package com.sourcelab.remoteHelper

import android.service.quicksettings.TileService
import android.widget.Toast

import android.content.Intent

import android.os.Handler
import android.service.quicksettings.Tile
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import java.io.ByteArrayOutputStream
import java.util.*

class TileShutdown: TileService(){

    fun executeRemoteCommand(username: String,
                             password: String,
                             hostname: String,
                             cmd: String,
                             port: Int): String {

        try {
            val jsch = JSch()
            val session = jsch.getSession(username, hostname, port)
            session.setPassword(password)

            // Avoid asking for key confirmation.
            val properties = Properties()
            properties.put("StrictHostKeyChecking", "no")
            session.setConfig(properties)

            session.connect()

            // Create SSH Channel.
            val sshChannel = session.openChannel("exec") as ChannelExec
            val outputStream = ByteArrayOutputStream()
            sshChannel.outputStream = outputStream

            // Execute command.
            sshChannel.setCommand(cmd)
            sshChannel.connect()

            // Sleep needed in order to wait long enough to get result back.
            Thread.sleep(1_000)
            sshChannel.disconnect()

            session.disconnect()

            return outputStream.toString()

        } catch (e: JSchException) {
//            e.printStackTrace()
            return e.message.toString()
        }
    }

    override fun onClick() {
        super.onClick()

        if (qsTile.state == Tile.STATE_INACTIVE) {
            qsTile.state = Tile.STATE_ACTIVE
            qsTile.updateTile()

            val cmd = AppPreferences.getVal("ssh_cmd")
            val host = AppPreferences.getVal("ssh_host")
            val name = AppPreferences.getVal("ssh_name")
            val pw = AppPreferences.getVal("ssh_pw")
            val port = AppPreferences.getVal("ssh_port")

            val uiHandler = Handler()

            // 收起通知栏
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            if (cmd == null || host == null || name == null || pw == null || cmd.isEmpty() || host.isEmpty() || name.isEmpty() || pw.isEmpty()) {
                Toast.makeText(this@TileShutdown, "SSH相关参数不完整，请先长按快捷设置按钮进入APP设置", Toast.LENGTH_SHORT)
                    .show()
            } else {
                AsyncUtils.doAsync {
                    val output = executeRemoteCommand(name, pw, host, cmd, if (port == null || port.isEmpty()) 22 else port.toInt())
                    uiHandler.post {
                        Toast.makeText(this@TileShutdown, if (output.isEmpty()) "命令已执行" else output, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
            uiHandler.postDelayed({
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.updateTile()
            }, 3000)
        }

        // Called when the user click the tile
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile.state = Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}