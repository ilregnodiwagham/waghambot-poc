import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent


@OptIn(PrivilegedIntent::class)
suspend fun main() {
    val kord = Kord(System.getenv("DISCORD_TOKEN"))

    kord.on<MessageCreateEvent> { // runs every time a message is created that our bot can read

        // ignore other bots, even ourselves. We only serve humans here!
        if (message.author?.isBot != false) return@on

        // check if our command is being invoked
        if (message.content != "!ping") return@on

        // all clear, give them the pong!
        message.channel.createMessage("pong!")
    }

    kord.login {
        intents += Intent.GuildMembers
        intents += Intent.GuildPresences
        intents += Intent.MessageContent
    }
}