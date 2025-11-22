package wtf.dettex;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import wtf.dettex.api.file.DirectoryCreator;
import wtf.dettex.api.file.FileController;
import wtf.dettex.api.file.FileRepository;
import wtf.dettex.api.file.exception.FileProcessingException;
import wtf.dettex.api.repository.box.BoxESPRepository;
import wtf.dettex.api.repository.rct.RCTRepository;
import wtf.dettex.api.repository.way.WayRepository;
import wtf.dettex.api.other.draggable.DraggableRepository;
import wtf.dettex.api.file.*;
import wtf.dettex.api.repository.macro.MacroRepository;
import wtf.dettex.event.EventManager;
import wtf.dettex.modules.api.ModuleProvider;
import wtf.dettex.modules.api.ModuleRepository;
import wtf.dettex.modules.api.ModuleSwitcher;
import wtf.dettex.api.system.sound.SoundManager;
import wtf.dettex.common.util.logger.LoggerUtil;
import wtf.dettex.common.util.render.ScissorManager;
import wtf.dettex.common.client.ClientInfo;
import wtf.dettex.common.client.ClientInfoProvider;
import wtf.dettex.common.client.IRCManager;
import wtf.dettex.common.listener.ListenerRepository;
import wtf.dettex.implement.features.commands.CommandDispatcher;
import wtf.dettex.implement.features.commands.manager.CommandRepository;
import wtf.dettex.implement.features.altmanager.AltManagerConfig;
import wtf.dettex.implement.features.altmanager.AltManagerScreen;
import wtf.dettex.modules.impl.combat.killaura.attack.AttackPerpetrator;
import wtf.dettex.implement.screen.menu.MenuScreen;
import wtf.dettex.implement.proxy.ProxyConnection;
import wtf.dettex.implement.features.pixiksystem2.telegram.JoinNotification;
import wtf.dettex.api.repository.theme.ThemeRepository;

import java.io.File;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Main implements ModInitializer {

    @Getter
    static Main instance;
    EventManager eventManager = new EventManager();
    ModuleRepository moduleRepository;
    ModuleSwitcher moduleSwitcher;
    IRCManager ircManager;
    CommandRepository commandRepository;
    CommandDispatcher commandDispatcher;
    BoxESPRepository boxESPRepository = new BoxESPRepository(eventManager);
    MacroRepository macroRepository = new MacroRepository(eventManager);
    WayRepository wayRepository = new WayRepository(eventManager);
    RCTRepository RCTRepository = new RCTRepository(eventManager);
    ModuleProvider moduleProvider;
    DraggableRepository draggableRepository;
    FileRepository fileRepository;
    FileController fileController;
    ScissorManager scissorManager = new ScissorManager();
    ClientInfoProvider clientInfoProvider;
    ListenerRepository listenerRepository;
    AttackPerpetrator attackPerpetrator = new AttackPerpetrator();
    ProxyConnection proxyConnection = new ProxyConnection();
    ThemeRepository themeRepository;
    //временно нахуй надо DeepLearningManager deepLearningManager;
    boolean initialized;


    @Override

    public void onInitialize() {
        instance = this;
        initClientInfoProvider();
        initModules();
        initDraggable();
        // initialize themes before file manager so ThemeFile can load
        themeRepository = new ThemeRepository();
        initFileManager();
        initCommands();
        initListeners();
        initIrcManager();
        SoundManager.init();
        MenuScreen menuScreen = new MenuScreen();
        menuScreen.initialize();
        JoinNotification.sendAsync();
        AltManagerConfig.loadAccountsAndApply(AltManagerScreen.ALTS);

        //временно нахуй надо deepLearningManager = new DeepLearningManager();
        initialized = true;
    }


    
    private void initDraggable() {
        draggableRepository = new DraggableRepository();
        draggableRepository.setup();
    }



    
    private void initModules() {
        moduleRepository = new ModuleRepository();
        moduleRepository.setup();
        moduleProvider = new ModuleProvider(moduleRepository.modules());
        moduleSwitcher = new ModuleSwitcher(moduleRepository.modules(), eventManager);
    }


    
    private void initCommands() {
        commandRepository = new CommandRepository();
        commandDispatcher = new CommandDispatcher(eventManager);
    }

    private void initClientInfoProvider() {
        File clientDirectory = new File(MinecraftClient.getInstance().runDirectory, "\\Dettex\\");
        File filesDirectory = new File(clientDirectory, "\\files\\");
        File moduleFilesDirectory = new File(filesDirectory, "\\config\\");
        clientInfoProvider = new ClientInfo("Dettex", "PixikDev", "ADMIN", clientDirectory, filesDirectory, moduleFilesDirectory);
    }

    private void initFileManager() {
        DirectoryCreator directoryCreator = new DirectoryCreator();
        directoryCreator.createDirectories(clientInfoProvider.clientDir(), clientInfoProvider.filesDir(), clientInfoProvider.configsDir());
        fileRepository = new FileRepository();
        fileRepository.setup(this);
        fileController = new FileController(fileRepository.getClientFiles(), clientInfoProvider.filesDir(), clientInfoProvider.configsDir());
        try {
            fileController.loadFiles();
        } catch (FileProcessingException e) {
            LoggerUtil.error("Error occurred while loading files: " + e.getMessage() + " " + e.getCause());
        }
    }

    
    private void initListeners() {
        listenerRepository = new ListenerRepository();
        listenerRepository.setup();
    }

    
    private void initIrcManager() {
        ircManager = new IRCManager();
        ircManager.connect("PixikDev");
    }

}
