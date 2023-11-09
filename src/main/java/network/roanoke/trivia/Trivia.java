package network.roanoke.trivia;

import com.sun.jna.platform.mac.Carbon;
import net.draycia.carbon.api.CarbonChat;
import net.draycia.carbon.api.CarbonChatProvider;
import net.draycia.carbon.api.CarbonServer;
import net.draycia.carbon.api.event.events.CarbonChatEvent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import network.roanoke.trivia.Commands.QuizCommands;
import network.roanoke.trivia.Quiz.QuizManager;
import network.roanoke.trivia.Utils.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class Trivia implements ModInitializer{

    /**
     * Runs the mod initializer.
     */
    public static final Logger LOGGER = LoggerFactory.getLogger("Trivia");
    public static FabricServerAudiences adventure;
    public static MiniMessage mm = MiniMessage.miniMessage();

    public static Messages messages = new Messages(FabricLoader.getInstance().getConfigDir().resolve("Trivia/messages.json"));
    public static Trivia instance;
    public QuizManager quiz = new QuizManager();
    public Config config = new Config();
    public Integer quizIntervalCounter = 0;
    public Integer quizTimeOutCounter = 0;



    @Override
    public void onInitialize() {

        instance = this;

        new QuizCommands();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            adventure = FabricServerAudiences.of(server);
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (!quiz.quizInProgress() && (server.getPlayerManager().getPlayerList().size() > 0)) {
                if (quizIntervalCounter >= config.getQuizInterval()) {
                    quizIntervalCounter = 0;
                    quiz.startQuiz(server);
                } else {
                    quizIntervalCounter++;
                }
            } else {
                if (quizTimeOutCounter >= config.getQuizTimeOut()) {
                    quizTimeOutCounter = 0;
                    quizIntervalCounter = 0;
                    quiz.timeOutQuiz(server); // move timeout message to this function later
                } else {
                    quizTimeOutCounter++;
                }

            }
        });
        CarbonChat carbon = CarbonChatProvider.carbonChat();

        carbon.eventHandler().subscribe(CarbonChatEvent.class, carbonchat-> {
            if (quiz.quizInProgress()){
                if (quiz.isRightAnswer(carbonchat.message().toString())){
                    LOGGER.info("Trivia question was answered correctly.");
                    quiz.processQuizWinner((ServerPlayerEntity) carbonchat,  (MinecraftServer)carbonchat.sender());
                }
            }
        });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (quiz.quizInProgress()) {
                if (quiz.isRightAnswer(message.getContent().getString())) {
                    LOGGER.info("Trivia question was answered correctly.");
                    quiz.processQuizWinner(sender, sender.server);}
            }
        });


    }

    public static Trivia getInstance() {
        return instance;
    }


}