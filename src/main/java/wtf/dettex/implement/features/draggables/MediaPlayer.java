package wtf.dettex.implement.features.draggables;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import wtf.dettex.api.other.draggable.AbstractDraggable;
import wtf.dettex.api.system.font.FontRenderer;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.other.BufferUtil;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.common.util.other.StopWatch;
import wtf.dettex.common.util.other.StringUtil;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.render.Render2DUtil;
import wtf.dettex.common.util.render.ScissorManager;
import wtf.dettex.Main;
import wtf.dettex.modules.impl.render.Hud;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaPlayer extends AbstractDraggable {
    public static MediaPlayer getInstance() {
        return Instance.getDraggable(MediaPlayer.class);
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MediaPlayerPoller");
        t.setDaemon(true);
        return t;
    });
    private MediaInfo mediaInfo = new MediaInfo("Название Трека", "Артист", new byte[0], 43, 150, false);
    private final Identifier artwork = Identifier.of("textures/xyu.png");
    private final StopWatch lastMedia = new StopWatch();
    public IMediaSession session;
    private float widthDuration;
    private int pollCounter = 0;
    private final java.util.concurrent.atomic.AtomicBoolean polling = new java.util.concurrent.atomic.AtomicBoolean(false);

    public MediaPlayer() {
        super("Media Player", 10, 400, 100, 40,true);
    }

    private Integer savedX, savedY, savedW, savedH;
    private boolean pendingRestore = false;

    @Override
    public boolean visible() {
        return !lastMedia.finished(2000) || PlayerIntersectionUtil.isChat(mc.currentScreen) || (mc.currentScreen instanceof wtf.dettex.implement.screen.mainmenu.CustomMainMenu);
    }

    @Override
    public void tick() {
        boolean wantDetect = (Hud.getInstance().isState() && Hud.getInstance().interfaceSettings.isSelected("Media Player"))
                || (mc.currentScreen instanceof wtf.dettex.implement.screen.mainmenu.CustomMainMenu);
        if (wantDetect) {
            int interval = (mc.currentScreen instanceof wtf.dettex.implement.screen.mainmenu.CustomMainMenu) ? 2 : 5;
            pollCounter = (pollCounter + 1) % interval;
            if (pollCounter != 0) return;
            if (!polling.compareAndSet(false, true)) return;
            executorService.submit(() -> {
                try {
                java.util.List<IMediaSession> sessions = MediaPlayerInfo.Instance.getMediaSessions();
                IMediaSession candidate = sessions.stream()
                        .filter(s -> {
                            try {
                                MediaInfo m = s.getMedia();
                                return !(m.getTitle().isEmpty() && m.getArtist().isEmpty());
                            } catch (Exception e) { return false; }
                        })
                        .findFirst()
                        .orElse(sessions.stream().findFirst().orElse(null));

                if (candidate != null) session = candidate;

                if (session != null) {
                    try {
                        MediaInfo info = session.getMedia();
                        if (info != null) {
                            if (mediaInfo.getTitle().equals("Название Трека") || !Arrays.toString(mediaInfo.getArtworkPng()).equals(Arrays.toString(info.getArtworkPng()))) {
                                BufferUtil.registerTexture(artwork, info.getArtworkPng());
                            }
                            mediaInfo = info;
                            lastMedia.reset();
                        }
                    } catch (Exception ignored) {}
                }
                } finally {
                    polling.set(false);
                }
            });
        }

        if (!(mc.currentScreen instanceof wtf.dettex.implement.screen.mainmenu.CustomMainMenu) && pendingRestore) {
            if (savedX != null && savedY != null && savedW != null && savedH != null) {
                setX(savedX);
                setY(savedY);
                setWidth(savedW);
                setHeight(savedH);
            }
            pendingRestore = false;
            savedX = savedY = savedW = savedH = null;
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        ScissorManager scissor = Main.getInstance().getScissorManager();
        FontRenderer big = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer mini = Fonts.getSize(12, Fonts.Type.DEFAULT);
        int sizeArtwork = 32;
        int sizePausePlay = 4;
        int maxDurationWidth = getWidth() - (sizeArtwork + 12);
        int duration = Math.max(1, (int) mediaInfo.getDuration());
        int position = MathHelper.clamp((int) mediaInfo.getPosition(),0, duration);
        String timeDuration = StringUtil.getDuration(duration);
        String currentTime = StringUtil.getDuration(position);
        widthDuration = MathHelper.clamp(
                MathUtil.interpolateSmooth(1, widthDuration, Math.round((float) position / duration * maxDurationWidth)),
                1, maxDurationWidth);

        blurGlass.render(ShapeProperties.create(matrix,getX(),getY(),getWidth(),getHeight()).thickness(2.25F).softness(1)
                .round(4).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(Hud.newHudAlpha.getValue())).build());

        scissor.push(matrix.peek().getPositionMatrix(),getX() + sizeArtwork + 8, getY(),getWidth() - sizeArtwork - 10,getHeight());
        big.drawStringWithScroll(matrix, mediaInfo.getTitle(), getX() + sizeArtwork + 8,getY() + 7, 56, ColorUtil.getText());
        mini.drawStringWithScroll(matrix, mediaInfo.getArtist(), getX() + sizeArtwork + 8, getY() + 15.5F, 56, ColorUtil.getText(0.75F));
        scissor.pop();

        Render2DUtil.drawTexture(context,artwork,getX() + 4,getY() + 4,sizeArtwork,3,sizeArtwork,sizeArtwork,sizeArtwork,ColorUtil.getRect(1));
        int leftTimeX = getX() + 8 + sizeArtwork;
        int timeY = getY() + 27;
        mini.drawString(matrix, currentTime, leftTimeX, timeY, ColorUtil.getText());

        int rightTimeX = getX() + getWidth() - 4 - (int) mini.getStringWidth(timeDuration);
        mini.drawString(matrix, timeDuration, rightTimeX, timeY, ColorUtil.getText());

        rectangle.render(ShapeProperties.create(matrix, getX() + 8 + sizeArtwork, getY() + getHeight() - 8, maxDurationWidth, 2)
                .round(0.75F).color(ColorUtil.getRect(0.75F)).build());

        rectangle.render(ShapeProperties.create(matrix, getX() + 8 + sizeArtwork, getY() + getHeight() - 8, widthDuration, 2)
                .softness(4).round(1).color(ColorUtil.roundClientColor(0.2F)).build());

        rectangle.render(ShapeProperties.create(matrix, getX() + 8 + sizeArtwork, getY() + getHeight() - 8, widthDuration, 2)
                .round(0.75F).color(ColorUtil.roundClientColor(1)).build());

        float contentStart = getX() + sizeArtwork + 8;
        float contentEnd = getX() + getWidth() - 4;
        float iconDrawX = contentStart + (contentEnd - contentStart) / 2f - sizePausePlay / 2f;
        boolean livePlaying = false;
        if (session != null) {
            try { livePlaying = session.getMedia().getPlaying(); } catch (Exception ignored) {}
        }
        String icon = (livePlaying || mediaInfo.getPlaying()) ? "pause" : "play";
        Render2DUtil.drawTexture(context,Identifier.of("textures/" + icon + ".png"), iconDrawX, getY() + 26,sizePausePlay);

        // Prev/Next text controls adjacent to time codes
        String prevTxt = "<<";
        String nextTxt = ">>";
        int prevX = leftTimeX + (int) mini.getStringWidth(currentTime) + 4;
        int nextX = rightTimeX - 4 - (int) mini.getStringWidth(nextTxt);
        int btnY = getY() + 24;
        mini.drawString(matrix, prevTxt, prevX, btnY + 2f, ColorUtil.getText());
        mini.drawString(matrix, nextTxt, nextX, btnY + 2f, ColorUtil.getText());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // In main menu there is no player; avoid snapping to other draggables and just clamp position
        if (mc.currentScreen instanceof wtf.dettex.implement.screen.mainmenu.CustomMainMenu) {
            float mouseDragX = mouseX + getDragX();
            float mouseDragY = mouseY + getDragY();
            int windowWidth = window.getScaledWidth();
            int windowHeight = window.getScaledHeight();
            setX((int) Math.max(0, Math.min(mouseDragX, windowWidth - getWidth())));
            setY((int) Math.max(0, Math.min(mouseDragY, windowHeight - getHeight())));
            drawDraggable(context);
            return;
        }
        super.render(context, mouseX, mouseY, delta);
    }

    public void renderAtMenu(DrawContext context, int x, int y, int w, int h, float delta) {
        if (savedW == null || savedH == null || savedX == null || savedY == null) {
            savedX = getX();
            savedY = getY();
            savedW = getWidth();
            savedH = getHeight();
        }
        pendingRestore = true;

        int ox = getX();
        int oy = getY();
        int ow = getWidth();
        int oh = getHeight();

        setX(x);
        setY(y);
        setWidth(w);
        setHeight(h);
        drawDraggable(context);
        setX(ox);
        setY(oy);
        setWidth(ow);
        setHeight(oh);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Hitbox for center play/pause aligned to icon draw position
            int sizeArtwork = 32;
            int sizePausePlay = 4;
            float contentStart = getX() + sizeArtwork + 8;
            float contentEnd = getX() + getWidth() - 4;
            float iconDrawX = contentStart + (contentEnd - contentStart) / 2f - sizePausePlay / 2f;
            int iconY = getY() + 26;
            int playHitW = 12;
            int playHitH = 12;
            int playX = Math.round(iconDrawX - (playHitW - sizePausePlay) / 2f);
            int playY = iconY - (playHitH - sizePausePlay) / 2;

            boolean onPlay = MathUtil.isHovered(mouseX, mouseY, playX, playY, playHitW, playHitH);

            // Hitboxes for prev/next near time labels
            FontRenderer mini = Fonts.getSize(12, Fonts.Type.DEFAULT);
            String timeDuration = StringUtil.getDuration((int) mediaInfo.getDuration());
            String currentTime = StringUtil.getDuration(MathHelper.clamp((int) mediaInfo.getPosition(), 0, (int) mediaInfo.getDuration()));

            int leftTimeX = getX() + 8 + sizeArtwork;
            int rightTimeX = getX() + getWidth() - 4 - (int) mini.getStringWidth(timeDuration);
            int btnY = getY() + 24;

            String prevTxt = "<<";
            String nextTxt = ">>";
            int prevTextX = leftTimeX + (int) mini.getStringWidth(currentTime) + 4;
            int nextTextX = rightTimeX - 4 - (int) mini.getStringWidth(nextTxt);
            int prevW = (int) mini.getStringWidth(prevTxt);
            int nextW = (int) mini.getStringWidth(nextTxt);
            int hitH = 12;

            boolean onPrev = MathUtil.isHovered(mouseX, mouseY, prevTextX, btnY, prevW, hitH);
            boolean onNext = MathUtil.isHovered(mouseX, mouseY, nextTextX, btnY, nextW, hitH);

            if (onPrev || onPlay || onNext) {
                if (session != null) {
                    if (onPrev) session.previous();
                    if (onPlay) session.playPause();
                    if (onNext) session.next();
                }
                return true;
            }
            if (mc.currentScreen instanceof wtf.dettex.implement.screen.mainmenu.CustomMainMenu) {
                return false;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
