package com.servermonitor.ssh

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.servermonitor.model.ServerConfig

class SshManager {

    @Throws(Exception::class)
    fun execute(server: ServerConfig, command: String, timeoutMs: Int = 20000): String {
        val jsch = JSch()
        jsch.addIdentity("key", server.privateKey.toByteArray(Charsets.UTF_8), null, null)

        var session: Session? = null
        var channel: ChannelExec? = null
        try {
            session = jsch.getSession(server.username, server.host, server.port)
            session.setConfig("StrictHostKeyChecking", "no")
            session.setConfig("PreferredAuthentications", "publickey")
            session.timeout = timeoutMs
            session.connect(timeoutMs)

            channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            channel.setPty(false)
            val input = channel.inputStream
            channel.connect(timeoutMs)
            return input.readBytes().toString(Charsets.UTF_8)
        } finally {
            channel?.disconnect()
            session?.disconnect()
        }
    }
}