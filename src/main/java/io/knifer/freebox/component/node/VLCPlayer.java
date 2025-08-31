package io.knifer.freebox.component.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.knifer.freebox.FreeBoxApplication;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.exception.FBException;
import io.knifer.freebox.handler.EpgFetchingHandler;
import io.knifer.freebox.handler.impl.ParameterizedEggFetchingHandler;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.SystemHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.helper.WindowHelper;
import io.knifer.freebox.model.common.diyp.Epg;
import io.knifer.freebox.model.domain.LiveChannel;
import io.knifer.freebox.model.domain.LiveChannelGroup;
import io.knifer.freebox.util.AsyncUtil;
import io.knifer.freebox.util.CastUtil;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.ValidationUtil;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Callback;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.ToggleSwitch;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.fullscreen.JavaFXFullScreenStrategy;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.annotation.Nullable;
import javax.swing.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * VLC播放器自定义组件
 * PS：我知道这里的代码很乱，没有注释，因为我没指望谁有心思动这个类。如果你有什么想法，直接告诉我。
 *
 * @author Knifer
 */
@Slf4j
public class VLCPlayer {

    private final Stage stage;
    private final Scene scene;
    private final AnchorPane controlPane;
    private final Timer controlPaneHideTimer = new Timer(2000, evt -> setControlsVisible(false));
    private final EmbeddedMediaPlayer mediaPlayer;
    private final ImageView videoImageView;
    private final ProgressIndicator loadingProgressIndicator;
    private final Label loadingProgressLabel;
    private final ImageView pausedPlayButtonImageView;
    private final Label loadingErrorIconLabel;
    private final Label loadingErrorLabel;
    private final VBox loadingErrorVBox;
    private final Label pauseLabel;
    private final Label stepBackwardLabel;
    private final Label stepForwardLabel;
    private final Slider volumeSlider;
    private final Label volumeLabel;
    private final ToggleGroup rateSettingToggleGroup;
    private final RadioButton rate0_5SettingRadioButton;
    private final RadioButton rate1SettingRadioButton;
    private final RadioButton rate1_25SettingRadioButton;
    private final RadioButton rate1_5SettingRadioButton;
    private final RadioButton rate2SettingRadioButton;
    private final ToggleSwitch fillWindowToggleSwitch;
    private final Button reloadButton;
    private final Label settingsLabel;
    private final HBox liveChannelLinesHBox;
    private final ProgressBar videoProgressBar;
    private final Label videoProgressLabel;
    private final Label videoProgressSplitLabel;
    private final Label videoProgressLengthLabel;
    private final Label fullScreenLabel;
    private final Label videoTitleLabel;
    private final StackPane playerPane;
    private final LiveChannelBanner liveChannelBanner;
    private final LiveDrawer liveChannelDrawer;
    private final EpgFetchingHandler epgFetchingHandler;
    private final DateTimeFormatter LOCAL_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final FontIcon pauseIcon = FontIcon.of(FontAwesome.PAUSE, 32, Color.WHITE);
    private final FontIcon playIcon = FontIcon.of(FontAwesome.PLAY, 32, Color.WHITE);
    private final FontIcon stepBackwardIcon = FontIcon.of(FontAwesome.STEP_BACKWARD, 32, Color.WHITE);
    private final FontIcon stepForwardIcon = FontIcon.of(FontAwesome.STEP_FORWARD, 32, Color.WHITE);
    private final FontIcon volumeOnIcon = FontIcon.of(FontAwesome.VOLUME_UP, 32, Color.WHITE);
    private final FontIcon volumeOffIcon = FontIcon.of(FontAwesome.VOLUME_OFF, 32, Color.WHITE);
    private final FontIcon fullScreenIcon = FontIcon.of(FontAwesome.ARROWS_ALT, 32, Color.WHITE);
    private final FontIcon reloadIcon = FontIcon.of(FontAwesome.REFRESH, 16);
    private final FontIcon settingsIcon = FontIcon.of(FontAwesome.SLIDERS, 32, Color.WHITE);
    private final AtomicLong videoLength = new AtomicLong(-1);
    private final AtomicLong initProgress = new AtomicLong(-1);
    private final AtomicBoolean isVideoProgressBarUsing = new AtomicBoolean(false);
    private final BooleanProperty isLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty isError = new SimpleBooleanProperty(false);
    private volatile boolean destroyFlag = false;

    private List<LiveChannelGroup> liveChannelGroups = null;
    private LiveInfoBO selectedLive = null;
    private LiveInfoBO playingLive = null;
    @Setter
    private String epgServiceUrl = null;

    private Runnable stepBackwardRunnable = BaseValues.EMPTY_RUNNABLE;
    private Runnable stepForwardRunnable = BaseValues.EMPTY_RUNNABLE;
    private Runnable fullScreenRunnable = BaseValues.EMPTY_RUNNABLE;
    private Runnable fullScreenExitRunnable = BaseValues.EMPTY_RUNNABLE;

    public VLCPlayer(HBox parent) {
        this(parent, null);
    }

    public VLCPlayer(HBox parent, @Nullable Config config) {
        boolean hasConfig = config != null;
        boolean liveMode = hasConfig && BooleanUtils.toBoolean(config.getLiveMode());
        ObservableList<Node> parentChildren = parent.getChildren();
        ReadOnlyDoubleProperty parentWidthProp = parent.widthProperty();
        DoubleBinding paneWidthProp = liveMode ? parentWidthProp.multiply(1) : parentWidthProp.multiply(0.8);
        ReadOnlyDoubleProperty parentHeightProp = parent.heightProperty();
        URL stylesheetUrl;
        List<Node> paneChildren;
        VBox volumePopOverInnerVBox;
        PopOver volumePopOver;
        Timer volumePopOverHideTimer;
        Label rateSettingTitleLabel;
        HBox rateSettingRadioButtonHBox;
        HBox rateSettingHBox;
        Label reloadSettingTitleLabel;
        HBox reloadSettingsHBox;
        VBox settingsPopOverInnerVBox;
        PopOver settingsPopOver;
        Timer settingsPopOverHideTimer;
        HBox progressLabelHBox;
        HBox leftToolBarHbox;
        HBox rightToolBarHbox;
        AnchorPane controlBottomAnchorPane;
        StackPane progressMiddleStackPane;
        AnchorPane controlTopAnchorPane;
        MediaPlayerFactory mediaPlayerFactory;

        stage = WindowHelper.getStage(parent);
        scene = stage.getScene();
        stylesheetUrl = FreeBoxApplication.class.getResource("css/player.css");
        if (stylesheetUrl == null) {
            throw new FBException("player.css not found");
        }
        scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        playerPane = new StackPane();
        stage.setFullScreenExitHint(StringUtils.EMPTY);
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        mediaPlayerFactory = Context.INSTANCE.isDebug() ?
                new MediaPlayerFactory(List.of("-vvv")) : new MediaPlayerFactory();
        mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
        mediaPlayer.fullScreen().strategy(new JavaFXFullScreenStrategy(stage){
            @Override
            public void onBeforeEnterFullScreen() {
                // 隐藏除播放器外的所有控件
                setOtherNodesVisible(false);
                // 绑定播放器宽度与父窗口宽度一致
                bindPlayerPaneWidth(parentWidthProp);
                fullScreenRunnable.run();
                parent.requestFocus();
            }

            @Override
            public void onAfterExitFullScreen() {
                // 显示所有控件
                setOtherNodesVisible(true);
                // 绑定非全屏下的播放器宽度
                bindPlayerPaneWidth(paneWidthProp);
                fullScreenExitRunnable.run();
            }

            private void setOtherNodesVisible(boolean visible) {
                parentChildren.forEach(p -> {
                    if (p != playerPane) {
                        p.setVisible(visible);
                    }
                });
            }
        });
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void mediaPlayerReady(MediaPlayer mediaPlayer) {
                setLoading(false);
            }

            @Override
            public void buffering(MediaPlayer mediaPlayer, float newCache) {
                Platform.runLater(() -> setLoading(newCache));
            }

            @Override
            public void paused(MediaPlayer mediaPlayer) {
                SystemHelper.allowSleep();
                Platform.runLater(() -> {
                    if (isLoading()) {
                        setLoading(false);
                    }
                    pausedPlayButtonImageView.setVisible(true);
                    pauseLabel.setGraphic(playIcon);
                });
            }

            @Override
            public void playing(MediaPlayer mediaPlayer) {
                SystemHelper.preventSleep();
                Platform.runLater(() -> {
                    if (isLoading()) {
                        setLoading(false);
                    }
                    if (pauseLabel.getGraphic() != pauseIcon) {
                        pauseLabel.setGraphic(pauseIcon);
                    }
                    if (pausedPlayButtonImageView.isVisible()) {
                        pausedPlayButtonImageView.setVisible(false);
                    }
                });
            }

            @Override
            @SuppressWarnings("ConstantConditions")
            public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
                long length = mediaPlayer.status().length();

                initProgress.getAndUpdate(val -> {
                    if (val != -1) {
                        Platform.runLater(() -> mediaPlayer.controls().setTime(val));
                    }

                    return -1;
                });
                if (liveMode) {

                    return;
                }
                Platform.runLater(() -> {
                    videoLength.set(length);
                    videoProgressLengthLabel.setText(formatProgressText(length));
                });
            }

            @Override
            @SuppressWarnings("ConstantConditions")
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                if (videoLength.get() > 0) {
                    Platform.runLater(() -> {
                        if (isLoading()) {
                            setLoading(false);
                        }
                        if (liveMode) {

                            return;
                        }
                        if (!isVideoProgressBarUsing.get()) {
                            videoProgressLabel.setText(formatProgressText(newTime));
                            videoProgressBar.setProgress(newTime / (double) videoLength.get());
                        }
                    });
                }
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                stepForwardRunnable.run();
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                SystemHelper.allowSleep();
                setError(true);
                setLoading(false);
            }
        });
        videoImageView = new ImageView();
        videoImageView.setPreserveRatio(true);
        videoImageView.fitWidthProperty().bind(playerPane.widthProperty());
        videoImageView.fitHeightProperty().bind(playerPane.heightProperty());
        mediaPlayer.videoSurface().set(new ImageViewVideoSurface(videoImageView));
        loadingProgressIndicator = new ProgressIndicator();
        loadingProgressIndicator.visibleProperty().bind(isLoading.and(isError.not()));
        loadingProgressLabel = new Label();
        loadingProgressLabel.setVisible(false);
        loadingProgressLabel.getStyleClass().add("dodge-blue");
        pausedPlayButtonImageView = new ImageView(BaseResources.PLAY_BUTTON_IMG);
        pausedPlayButtonImageView.setFitWidth(64);
        pausedPlayButtonImageView.setFitHeight(64);
        pausedPlayButtonImageView.setPreserveRatio(true);
        pausedPlayButtonImageView.setVisible(false);
        loadingErrorIconLabel = new Label();
        loadingErrorIconLabel.setGraphic(FontIcon.of(FontAwesome.WARNING, 32, Color.WHITE));
        loadingErrorLabel = new Label(I18nHelper.get(I18nKeys.COMMON_VIDEO_LOADING_ERROR));
        loadingErrorLabel.getStyleClass().add("vlc-player-loading-error-label");
        loadingErrorVBox = new VBox(loadingErrorIconLabel, loadingErrorLabel);
        loadingErrorVBox.setSpacing(3);
        loadingErrorVBox.setAlignment(Pos.CENTER);
        loadingErrorVBox.visibleProperty().bind(isLoading.not().and(isError));
        // 暂停设置
        pauseLabel = new Label();
        pauseLabel.setGraphic(pauseIcon);
        pauseLabel.getStyleClass().add("vlc-player-control-label");
        pauseLabel.setOnMouseClicked(evt -> changePlayStatus());
        // 上一集、下一集
        stepBackwardLabel = new Label();
        stepBackwardLabel.setGraphic(stepBackwardIcon);
        stepBackwardLabel.getStyleClass().add("vlc-player-control-label");
        stepForwardLabel = new Label();
        stepForwardLabel.setGraphic(stepForwardIcon);
        stepForwardLabel.getStyleClass().add("vlc-player-control-label");
        if (liveMode) {
            epgFetchingHandler = new ParameterizedEggFetchingHandler();
            selectedLive = new LiveInfoBO();
            playingLive = new LiveInfoBO();
            stepBackwardLabel.setOnMouseClicked(evt -> {
                LiveChannelGroup channelGroup;
                int groupIdx;
                List<LiveChannel> liveChannels;
                LiveChannel liveChannel;
                int currentLiveChannelIdx;
                int channelIdx;

                if (CollectionUtil.isNotEmpty(liveChannelGroups)) {
                    channelGroup = playingLive.getLiveChannelGroup();
                    if (channelGroup != null) {
                        groupIdx = liveChannelGroups.indexOf(channelGroup);
                        liveChannels = channelGroup.getChannels();
                        liveChannel = playingLive.getLiveChannel();
                        currentLiveChannelIdx = liveChannels.indexOf(liveChannel);
                        if (currentLiveChannelIdx == 0) {
                            groupIdx--;
                            channelGroup = groupIdx > -1 ? CollUtil.get(liveChannelGroups, groupIdx) : null;
                        }
                        if (channelGroup != null) {
                            liveChannels = channelGroup.getChannels();
                            liveChannel = CollUtil.get(liveChannels, currentLiveChannelIdx);
                            channelIdx = currentLiveChannelIdx == 0 ?
                                    liveChannels.size() - 1 : currentLiveChannelIdx - 1;
                            if (liveChannel != null && CollUtil.get(liveChannels, channelIdx) != null) {
                                play(groupIdx, channelIdx, 0);
                            }
                        }
                    }
                    stepBackwardRunnable.run();
                }
            });
            stepForwardLabel.setOnMouseClicked(evt -> {
                LiveChannelGroup channelGroup;
                int groupIdx;
                List<LiveChannel> liveChannels;
                LiveChannel liveChannel;
                int currentLiveChannelIdx;
                int channelIdx;

                if (CollectionUtil.isNotEmpty(liveChannelGroups)) {
                    channelGroup = playingLive.getLiveChannelGroup();
                    if (channelGroup != null) {
                        groupIdx = liveChannelGroups.indexOf(channelGroup);
                        liveChannels = channelGroup.getChannels();
                        liveChannel = playingLive.getLiveChannel();
                        currentLiveChannelIdx = liveChannels.indexOf(liveChannel);
                        if (currentLiveChannelIdx == liveChannels.size() - 1) {
                            groupIdx++;
                            channelGroup = CollUtil.get(liveChannelGroups, groupIdx);
                            currentLiveChannelIdx = -1;
                        }
                        if (channelGroup != null) {
                            liveChannels = channelGroup.getChannels();
                            liveChannel = CollUtil.get(liveChannels, currentLiveChannelIdx);
                            channelIdx = currentLiveChannelIdx + 1;
                            if (liveChannel != null && CollUtil.get(liveChannels, channelIdx) != null) {
                                play(groupIdx, channelIdx, 0);
                            }
                        }
                    }
                }
                stepForwardRunnable.run();
            });
        } else {
            epgFetchingHandler = null;
            stepBackwardLabel.setOnMouseClicked(evt -> stepBackwardRunnable.run());
            stepForwardLabel.setOnMouseClicked(evt -> stepForwardRunnable.run());
        }
        // 音量设置
        volumeLabel = new Label();
        volumeLabel.setGraphic(volumeOnIcon);
        volumeLabel.getStyleClass().add("vlc-player-control-label");
        volumeLabel.setOnMouseClicked(evt -> {
            volumeLabel.setGraphic(mediaPlayer.audio().isMute() ? volumeOnIcon : volumeOffIcon);
            mediaPlayer.audio().mute();
        });
        volumeSlider = new Slider(0, 100, 100);
        volumeSlider.setOrientation(Orientation.VERTICAL);
        volumePopOverInnerVBox = new VBox(volumeSlider);
        volumePopOverInnerVBox.setAlignment(Pos.CENTER);
        volumePopOver = new PopOver(volumePopOverInnerVBox);
        volumePopOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
        volumePopOver.getStyleClass().add("vlc-player-pop-over");
        volumePopOver.setDetachable(false);
        volumePopOverHideTimer = new Timer(1000, evt -> volumePopOver.hide());
        volumePopOverInnerVBox.addEventFilter(MouseEvent.ANY, evt -> {
            EventType<? extends MouseEvent> eventType = evt.getEventType();

            if (eventType == MouseEvent.MOUSE_ENTERED) {
                setControlsAutoHide(false);
                volumePopOverHideTimer.stop();
                if (!volumePopOver.isShowing()) {
                    volumePopOver.show(volumeLabel);
                }
            } else if (eventType == MouseEvent.MOUSE_EXITED) {
                setControlsAutoHide(true);
                volumePopOverHideTimer.start();
            }
        });
        volumeSlider.valueProperty().addListener((ob, oldVal, newVal) -> {
            mediaPlayer.audio().setVolume(newVal.intValue());
            if (mediaPlayer.audio().isMute()) {
                mediaPlayer.audio().mute();
                volumeLabel.setGraphic(volumeOnIcon);
            }
        });
        volumeLabel.setOnMouseEntered(evt -> {
            volumePopOverHideTimer.restart();
            if (!volumePopOver.isShowing()) {
                volumePopOver.show(volumeLabel);
            }
        });
        settingsLabel = new Label();
        settingsLabel.getStyleClass().add("vlc-player-control-label");
        settingsLabel.setGraphic(settingsIcon);
        // 倍速设置
        rateSettingTitleLabel = new Label(I18nHelper.get(I18nKeys.VIDEO_SETTINGS_RATE));
        HBox.setMargin(rateSettingTitleLabel, new Insets(0, 10, 0, 0));
        rateSettingToggleGroup = new ToggleGroup();
        rate0_5SettingRadioButton = new RadioButton("0.5");
        rate0_5SettingRadioButton.setUserData(0.5f);
        rate0_5SettingRadioButton.setToggleGroup(rateSettingToggleGroup);
        rate1SettingRadioButton = new RadioButton("1.0");
        rate1SettingRadioButton.setUserData(1.0f);
        rate1SettingRadioButton.setToggleGroup(rateSettingToggleGroup);
        rate1_25SettingRadioButton = new RadioButton("1.25");
        rate1_25SettingRadioButton.setUserData(1.25f);
        rate1_25SettingRadioButton.setToggleGroup(rateSettingToggleGroup);
        rate1_5SettingRadioButton = new RadioButton("1.5");
        rate1_5SettingRadioButton.setUserData(1.5f);
        rate1_5SettingRadioButton.setToggleGroup(rateSettingToggleGroup);
        rate2SettingRadioButton = new RadioButton("2.0");
        rate2SettingRadioButton.setUserData(2.0f);
        rate2SettingRadioButton.setToggleGroup(rateSettingToggleGroup);
        // 默认选择1倍速
        rateSettingToggleGroup.selectToggle(rate1SettingRadioButton);
        rateSettingToggleGroup.selectedToggleProperty().addListener(((observable, oldValue, newValue) ->
            mediaPlayer.controls().setRate((float) newValue.getUserData())
        ));
        rateSettingRadioButtonHBox = new HBox(
                rate0_5SettingRadioButton,
                rate1SettingRadioButton,
                rate1_25SettingRadioButton,
                rate1_5SettingRadioButton,
                rate2SettingRadioButton
        );
        rateSettingHBox = new HBox(rateSettingTitleLabel, rateSettingRadioButtonHBox);
        rateSettingHBox.setSpacing(10);
        rateSettingHBox.setAlignment(Pos.CENTER);
        rateSettingRadioButtonHBox.setAlignment(Pos.CENTER);
        rateSettingRadioButtonHBox.setSpacing(5);
        // 铺满设置按钮
        fillWindowToggleSwitch = new ToggleSwitch(I18nHelper.get(I18nKeys.VIDEO_SETTINGS_FILL_WINDOW));
        fillWindowToggleSwitch.setFocusTraversable(false);
        videoImageView.preserveRatioProperty().bind(fillWindowToggleSwitch.selectedProperty().not());
        // 重新加载
        reloadSettingTitleLabel = new Label(I18nHelper.get(I18nKeys.VIDEO_SETTINGS_RELOAD));
        reloadButton = new Button();
        reloadButton.setGraphic(reloadIcon);
        reloadButton.setFocusTraversable(false);
        reloadButton.setOnAction(evt -> {
            initProgress.set(getCurrentProgress());
            mediaPlayer.controls().stop();
            mediaPlayer.controls().play();
        });
        reloadSettingsHBox = new HBox(reloadSettingTitleLabel, reloadButton);
        reloadSettingsHBox.setSpacing(15);
        reloadSettingsHBox.setAlignment(Pos.CENTER_LEFT);
        // 设置弹出框
        settingsPopOver = new PopOver();
        settingsPopOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
        settingsPopOver.getStyleClass().add("vlc-player-pop-over");
        settingsPopOver.setDetachable(false);
        settingsPopOverHideTimer = new Timer(1000, evt -> settingsPopOver.hide());
        settingsPopOverInnerVBox = new VBox(reloadSettingsHBox, fillWindowToggleSwitch, rateSettingHBox);
        settingsPopOverInnerVBox.setSpacing(10.0);
        settingsPopOverInnerVBox.addEventFilter(MouseEvent.ANY, evt -> {
            EventType<? extends MouseEvent> eventType = evt.getEventType();

            if (eventType == MouseEvent.MOUSE_ENTERED) {
                setControlsAutoHide(false);
                settingsPopOverHideTimer.stop();
                if (!settingsPopOver.isShowing()) {
                    settingsPopOver.show(settingsLabel);
                }
            } else if (eventType == MouseEvent.MOUSE_EXITED) {
                setControlsAutoHide(true);
                settingsPopOverHideTimer.start();
            }
        });
        settingsPopOver.setContentNode(settingsPopOverInnerVBox);
        settingsLabel.setOnMouseEntered(evt -> {
            settingsPopOverHideTimer.restart();
            if (!settingsPopOver.isShowing()) {
                settingsPopOver.show(settingsLabel);
            }
        });
        // 铺满、全屏组件
        fullScreenLabel = new Label();
        fullScreenLabel.getStyleClass().add("vlc-player-control-label");
        fullScreenLabel.setGraphic(fullScreenIcon);
        fullScreenLabel.setOnMouseClicked(evt -> mediaPlayer.fullScreen().toggle());
        rightToolBarHbox = new HBox(fullScreenLabel);
        rightToolBarHbox.setSpacing(20);
        if (liveMode) {
            // 直播模式，不用显示进度条相关组件
            videoProgressBar = null;
            videoProgressLabel = null;
            videoProgressSplitLabel = null;
            videoProgressLengthLabel = null;
            liveChannelLinesHBox = new HBox();
            liveChannelLinesHBox.setSpacing(8);
            leftToolBarHbox = new HBox(
                    pauseLabel, stepBackwardLabel, stepForwardLabel, volumeLabel, settingsLabel, liveChannelLinesHBox
            );
            controlBottomAnchorPane = new AnchorPane(leftToolBarHbox, rightToolBarHbox);
        } else {
            liveChannelLinesHBox = null;
            // 进度条组件
            videoProgressBar = new ProgressBar(0);
            videoProgressLabel = new Label("00:00:00");
            videoProgressLabel.getStyleClass().add("vlc-player-progress-label");
            videoProgressBar.setOnMousePressed(evt -> {
                double newProgress;

                if (!mediaPlayer.status().isPlayable() || !mediaPlayer.status().isSeekable()) {
                    return;
                }
                isVideoProgressBarUsing.set(true);
                // 让播放器的控制面板保持可见
                setControlsAutoHide(false);
                // 处理进度拖动相关逻辑
                newProgress = evt.getX() / videoProgressBar.getWidth();
                videoProgressBar.setProgress(newProgress);
                if (newProgress > 0) {
                    videoProgressLabel.setText(formatProgressText((long) (videoLength.get() * newProgress)));
                }
            });
            videoProgressBar.setOnMouseDragged(evt -> {
                double newX;
                double width;
                double newProgress;

                if (!mediaPlayer.status().isPlayable() || !mediaPlayer.status().isSeekable()) {
                    return;
                }
                newX = evt.getX();
                width = videoProgressBar.getWidth();
                newProgress = newX / width;
                if (newProgress > 0) {
                    if (newProgress > 1) {
                        videoProgressLabel.setText(formatProgressText(videoLength.get()));
                        videoProgressBar.setProgress(1);
                    } else {
                        videoProgressLabel.setText(formatProgressText((long) (videoLength.get() * newProgress)));
                        videoProgressBar.setProgress(newProgress);
                    }
                } else {
                    videoProgressLabel.setText("00:00:00");
                    videoProgressBar.setProgress(0);
                }
            });
            videoProgressBar.setOnMouseReleased(evt -> {
                if (!mediaPlayer.status().isPlayable() || !mediaPlayer.status().isSeekable()) {
                    return;
                }
                setLoading(true);
                // 恢复播放器的控制面板自动隐藏逻辑
                setControlsAutoHide(true);
                // 处理进度拖动相关逻辑
                mediaPlayer.controls().setPosition(((float) videoProgressBar.getProgress()));
                isVideoProgressBarUsing.set(false);
            });
            videoProgressBar.disableProperty().bind(isLoading.and(isError.not()));
            videoProgressSplitLabel = new Label("/");
            videoProgressSplitLabel.getStyleClass().add("vlc-player-progress-label");
            videoProgressLengthLabel = new Label("-:-:-");
            videoProgressLengthLabel.getStyleClass().add("vlc-player-progress-label");
            progressLabelHBox = new HBox(videoProgressLabel, videoProgressSplitLabel, videoProgressLengthLabel);
            progressLabelHBox.setSpacing(5);
            progressLabelHBox.setAlignment(Pos.CENTER);
            leftToolBarHbox = new HBox(pauseLabel, stepForwardLabel, volumeLabel, settingsLabel, progressLabelHBox);
            controlBottomAnchorPane = new AnchorPane(leftToolBarHbox, videoProgressBar, rightToolBarHbox);
            AnchorPane.setLeftAnchor(videoProgressBar, 490.0);
            AnchorPane.setRightAnchor(videoProgressBar, 70.0);
            AnchorPane.setTopAnchor(videoProgressBar, 10.0);
            AnchorPane.setBottomAnchor(videoProgressBar, 10.0);
        }
        leftToolBarHbox.setSpacing(20);
        leftToolBarHbox.setAlignment(Pos.CENTER);
        controlBottomAnchorPane.getStyleClass().add("vlc-player-anchor-pane");
        controlBottomAnchorPane.setOnMouseClicked(Event::consume);
        AnchorPane.setLeftAnchor(leftToolBarHbox, 10.0);
        AnchorPane.setTopAnchor(leftToolBarHbox, 10.0);
        AnchorPane.setBottomAnchor(leftToolBarHbox, 10.0);
        AnchorPane.setRightAnchor(rightToolBarHbox, 10.0);
        AnchorPane.setTopAnchor(rightToolBarHbox, 10.0);
        AnchorPane.setBottomAnchor(rightToolBarHbox, 10.0);
        // 顶端标题
        videoTitleLabel = new Label();
        videoTitleLabel.getStyleClass().add("vlc-player-title");
        controlTopAnchorPane = new AnchorPane(videoTitleLabel);
        controlTopAnchorPane.getStyleClass().add("vlc-player-anchor-pane");
        controlTopAnchorPane.setOnMouseClicked(Event::consume);
        AnchorPane.setLeftAnchor(videoTitleLabel, 10.0);
        AnchorPane.setRightAnchor(videoTitleLabel, 10.0);
        AnchorPane.setTopAnchor(videoTitleLabel, 10.0);
        AnchorPane.setBottomAnchor(videoTitleLabel, 10.0);
        // 摆放布局组件
        controlPane = new AnchorPane(controlBottomAnchorPane, controlTopAnchorPane);
        AnchorPane.setLeftAnchor(controlBottomAnchorPane, 0.0);
        AnchorPane.setRightAnchor(controlBottomAnchorPane, 0.0);
        AnchorPane.setBottomAnchor(controlBottomAnchorPane, 0.0);
        AnchorPane.setLeftAnchor(controlTopAnchorPane, 0.0);
        AnchorPane.setRightAnchor(controlTopAnchorPane, 0.0);
        AnchorPane.setTopAnchor(controlTopAnchorPane, 0.0);
        progressMiddleStackPane = new StackPane(
                loadingProgressIndicator,
                loadingProgressLabel,
                pausedPlayButtonImageView,
                loadingErrorVBox
        );
        paneChildren = playerPane.getChildren();
        paneChildren.add(videoImageView);
        paneChildren.add(progressMiddleStackPane);
        paneChildren.add(controlPane);
        if (liveMode) {
            liveChannelBanner = new LiveChannelBanner();
            StackPane.setMargin(liveChannelBanner, new Insets(0, 0, 50, 0));
            StackPane.setAlignment(liveChannelBanner, Pos.BOTTOM_CENTER);
            paneChildren.add(liveChannelBanner);
            liveChannelDrawer = new LiveDrawer(selectedLive, playingLive, playerPane, this);
            liveChannelDrawer.addEventFilter(MouseEvent.ANY, evt -> {
                EventType<? extends MouseEvent> eventType = evt.getEventType();

                if (eventType == MouseEvent.MOUSE_ENTERED) {
                    setControlsAutoHide(false);
                } else if (eventType == MouseEvent.MOUSE_EXITED) {
                    setControlsAutoHide(true);
                } else if (eventType == MouseEvent.MOUSE_MOVED) {
                    // 消费掉鼠标移动事件，避免controls被唤起
                    evt.consume();
                }
            });
            paneChildren.add(liveChannelDrawer);
            StackPane.setAlignment(liveChannelDrawer, Pos.CENTER_LEFT);
        } else {
            liveChannelBanner = null;
            liveChannelDrawer = null;
        }
        playerPane.getStyleClass().add("vlc-player");
        bindPlayerPaneWidth(paneWidthProp);
        playerPane.prefHeightProperty().bind(parentHeightProp);
        playerPane.minHeightProperty().bind(parentHeightProp);
        playerPane.maxHeightProperty().bind(parentHeightProp);
        playerPane.setOnMouseClicked(evt -> {
            if (evt.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (evt.getClickCount() == 1) {
                changePlayStatus();
            } else {
                changePlayStatus();
                mediaPlayer.fullScreen().toggle();
            }
        });
        // 键盘快捷键事件绑定
        parent.addEventFilter(KeyEvent.KEY_PRESSED, evt -> {
            switch (evt.getCode()) {
                case SPACE -> changePlayStatus();
                case ESCAPE -> {
                    if (mediaPlayer.fullScreen().isFullScreen()) {
                        mediaPlayer.fullScreen().toggle();
                    }
                }
                case F -> mediaPlayer.fullScreen().toggle();
                case Z -> fillWindowToggleSwitch.setSelected(!fillWindowToggleSwitch.isSelected());
                case RIGHT -> movePosition(true);
                case LEFT -> movePosition(false);
                case UP -> moveVolume(true);
                case DOWN -> moveVolume(false);
            }
        });
        // 鼠标移动事件处理
        parent.addEventHandler(MouseEvent.MOUSE_MOVED, evt -> setControlsVisible(true));
        parentChildren.add(0, playerPane);
        parent.requestFocus();
        setLoading(true);
    }

    private void movePosition(boolean forward) {
        long length = videoLength.get();
        long oldTime;
        long newTime;

        if (!mediaPlayer.status().isPlayable() || length <= 0) {
            return;
        }
        oldTime = mediaPlayer.status().time();
        newTime = forward ? Math.min(oldTime + 5000, length) : Math.max(oldTime - 5000, 0);
        mediaPlayer.controls().setTime(newTime);
    }

    private void moveVolume(boolean forward) {
        double oldVolume = volumeSlider.getValue();
        double newVolume = forward ? Math.min(oldVolume + 10, 100) : Math.max(oldVolume - 10, 0);

        if (mediaPlayer.audio().isMute()) {
            mediaPlayer.audio().mute();
            volumeLabel.setGraphic(volumeOnIcon);
        }
        volumeSlider.setValue(newVolume);
    }

    private void setControlsAutoHide(boolean flag) {
        if (flag) {
            controlPaneHideTimer.restart();
        } else {
            setControlsVisible(true);
            controlPaneHideTimer.stop();
        }
    }

    private void setControlsVisible(boolean flag) {
        Cursor cursor = scene.getCursor();

        if (controlPane.isVisible() != flag) {
            controlPane.setVisible(flag);
        }
        if (liveChannelDrawer != null) {
            liveChannelDrawer.setVisible(flag);
        }
        if (flag) {
            controlPaneHideTimer.restart();
            if (cursor == Cursor.NONE) {
                scene.setCursor(Cursor.DEFAULT);
            }
        } else {
            if (cursor != Cursor.NONE) {
                scene.setCursor(Cursor.NONE);
            }
        }
    }

    private void setLoading(float bufferCached) {
        loadingProgressLabel.setText(String.format("%.1f%%", bufferCached));
        if (bufferCached >= 100) {
            setLoading(false);

            return;
        }
        setLoading(true);
        if (!loadingProgressLabel.isVisible()) {
            loadingProgressLabel.setVisible(true);
        }
    }

    private void setLoading(boolean loading) {
        isLoading.set(loading);
        if (loading) {
            setError(false);
        }
        if (!loading && loadingProgressLabel.isVisible() && !isError()) {
            loadingProgressLabel.setVisible(false);
        }
    }

    private void setError(boolean flag) {
        isError.set(flag);
    }

    private boolean isError() {
        return isError.get();
    }

    private void bindPlayerPaneWidth(DoubleExpression widthProp) {
        DoubleProperty prefWidthProperty = playerPane.prefWidthProperty();
        DoubleProperty maxWidthProperty = playerPane.maxWidthProperty();
        DoubleProperty minWidthProperty = playerPane.minWidthProperty();

        if (prefWidthProperty.isBound()) {
            prefWidthProperty.unbind();
        }
        prefWidthProperty.bind(widthProp);
        if (maxWidthProperty.isBound()) {
            maxWidthProperty.unbind();
        }
        maxWidthProperty.bind(widthProp);
        if (minWidthProperty.isBound()) {
            minWidthProperty.unbind();
        }
        minWidthProperty.bind(widthProp);
    }

    public void play(
            String url,
            Map<String, String> headers,
            String videoTitle,
            @Nullable Long progress
    ) {
        String[] options;

        if (destroyFlag) {
            return;
        }
        setLoading(true);
        if (progress != null) {
            initProgress.set(Math.max(progress, -1));
        }
        setVideoTitle(videoTitle);
        options = parsePlayOptionsFromHeaders(headers);
        log.info("play url={}, options={}", url, options);
        AsyncUtil.execute(() -> {
            playMedia(url, options);
            mediaPlayer.audio().setVolume((int) volumeSlider.getValue());
        });
    }

    private synchronized void playMedia(String url, String[] options) {
        if (destroyFlag) {

            return;
        }
        mediaPlayer.media().play(url, options);
    }

    @Nullable
    private String[] parsePlayOptionsFromHeaders(Map<String, String> headers) {
        String userAgent;
        String referer;
        short size;

        if (headers.isEmpty()) {
            return new String[] {":http-range-length=1048576"};
        }
        userAgent = null;
        referer = null;
        size = 0;
        for (Map.Entry<String, String> keyValue : headers.entrySet()) {
            if (StringUtils.equalsIgnoreCase(keyValue.getKey(), "User-Agent")) {
                size++;
                userAgent = ":http-user-agent=" + keyValue.getValue();
            } else if (StringUtils.equalsIgnoreCase(keyValue.getKey(), "Referer")) {
                size++;
                referer = ":http-referer=" + keyValue.getValue();
            }
        }
        switch (size) {
            case 0:
                return null;
            case 1:
                if (userAgent != null) {
                    return new String[]{userAgent};
                } else {
                    return new String[]{referer};
                }
            default:
                return new String[]{userAgent, referer};
        }
    }

    public void setVideoTitle(String videoTitle) {
        videoTitleLabel.setText(videoTitle);
    }

    public void changePlayStatus() {
        if (!mediaPlayer.status().canPause() || isError() || isLoading()) {

            return;
        }
        // 调用暂停API时可能出现短暂延迟，为用户体验考虑，显示一下loading告知用户等待
        setLoading(true);
        mediaPlayer.controls().pause();
    }

    private boolean isLoading() {
        return isLoading.get();
    }

    private String formatProgressText(long totalMilliseconds) {
        // 将毫秒转换为秒
        long totalSeconds = totalMilliseconds / 1000;

        // 计算小时、分钟和秒
        int hours = (int) (totalSeconds / 3600);
        int minutes = (int) ((totalSeconds % 3600) / 60);
        int seconds = (int) (totalSeconds % 60);

        // 格式化字符串
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void stop() {
        setLoading(false);
        AsyncUtil.execute(() -> {
            State playerState = mediaPlayer.status().state();

            if (playerState != State.STOPPED && playerState != State.ENDED) {
                mediaPlayer.controls().stop();
            }
        });
    }

    public void setOnStepBackward(Runnable runnable) {
        this.stepBackwardRunnable = runnable;
    }

    public void setOnStepForward(Runnable runnable) {
        this.stepForwardRunnable = runnable;
    }

    public void setOnFullScreen(Runnable runnable) {
        this.fullScreenRunnable = runnable;
    }

    public void setOnFullScreenExit(Runnable runnable) {
        this.fullScreenExitRunnable = runnable;
    }

    public long getCurrentProgress() {
        long length = videoLength.get();
        float position;

        if (length < 1) {
            return 0;
        }
        position = mediaPlayer.status().position();

        return position == 0 ? 0 : ((long) (position * length));
    }

    public void destroy() {
        destroyFlag = true;
        if (mediaPlayer.status().isPlaying()) {
            SystemHelper.allowSleep();
        }
        mediaPlayer.release();
        log.info("vlc media player released");
    }

    public void setLiveChannelGroups(List<LiveChannelGroup> liveChannelGroups) {
        this.liveChannelGroups = liveChannelGroups;
        this.liveChannelDrawer.setLiveChannelGroups(liveChannelGroups);
    }

    public void play(int liveChannelGroupIdx, int liveChannelIdx, int liveChannelLineIdx) {
        LiveChannelGroup liveChannelGroup = liveChannelGroups.get(liveChannelGroupIdx);
        LiveChannel liveChannel = liveChannelGroup.getChannels().get(liveChannelIdx);
        LiveChannel.Line liveChannelLine = liveChannel.getLines().get(liveChannelLineIdx);

        play(liveChannelGroup, liveChannel, liveChannelLine);
    }

    private void play(LiveChannelGroup liveChannelGroup, LiveChannel liveChannel, LiveChannel.Line liveChannelLine) {
        LiveChannel lastPlayingLiveChannel = playingLive.getLiveChannel();

        playingLive.setLiveChannelGroup(liveChannelGroup);
        playingLive.setLiveChannel(liveChannel);
        playingLive.setLiveChannelLine(liveChannelLine);
        liveChannelDrawer.select(liveChannelGroup, liveChannel);

        play(liveChannelLine.getUrl(), Map.of(), liveChannel.getTitle(), null);
        if (lastPlayingLiveChannel != liveChannel) {
            // 切换频道时，显示banner（同一个频道切换线路时不显示）
            showLiveChannelBanner(liveChannel, liveChannelLine);
        }
        updateLiveChannelLinesHBox(lastPlayingLiveChannel, liveChannel, liveChannelLine);
    }

    private void updateLiveChannelLinesHBox(
            @Nullable LiveChannel lastPlayingLiveChannel, LiveChannel liveChannel, LiveChannel.Line liveChannelLine
    ) {
        ObservableList<Node> liveChannelLinesHBoxChildren = liveChannelLinesHBox.getChildren();
        List<LiveChannel.Line> liveChannelLines = liveChannel.getLines();
        boolean showLiveChannelLinesHBox = liveChannelLines.size() > 1;
        Label liveChannelLineLabel;
        LiveChannel.Line playingiveChannelLine;
        List<String> lineLabelStyleClasses;

        if (lastPlayingLiveChannel == liveChannel) {
            playingiveChannelLine = liveChannelLine;
        } else {
            liveChannelLinesHBoxChildren.clear();
            if (showLiveChannelLinesHBox) {
                for (LiveChannel.Line line : liveChannelLines) {
                    liveChannelLineLabel = new Label(line.getTitle());
                    liveChannelLineLabel.getStyleClass().add("vlc-player-live-channel-line-label");
                    liveChannelLineLabel.setUserData(line);
                    liveChannelLineLabel.setOnMouseClicked(
                            evt -> {
                                LiveChannel.Line lastPlayingLiveChannelLine = playingLive.getLiveChannelLine();

                                if (evt.getButton() != MouseButton.PRIMARY || line == lastPlayingLiveChannelLine) {

                                    return;
                                }
                                play(playingLive.getLiveChannelGroup(), liveChannel, line);
                            }
                    );
                    liveChannelLinesHBoxChildren.add(liveChannelLineLabel);
                }
            }
            playingiveChannelLine = CollectionUtil.getFirst(liveChannelLines);
        }
        if (playingiveChannelLine == null || !showLiveChannelLinesHBox) {

            return;
        }
        for (Node lineLabel : liveChannelLinesHBoxChildren) {
            lineLabelStyleClasses = lineLabel.getStyleClass();
            if (lineLabel.getUserData() == playingiveChannelLine) {
                // 为正在播放的线路标签添加样式
                lineLabelStyleClasses.remove("vlc-player-live-channel-line-label");
                if (!lineLabelStyleClasses.contains("vlc-player-live-channel-line-label-focus")) {
                    lineLabelStyleClasses.add("vlc-player-live-channel-line-label-focused");
                }
            } else {
                // 移除其他线路标签的样式
                lineLabelStyleClasses.remove("vlc-player-live-channel-line-label-focused");
                if (!lineLabelStyleClasses.contains("vlc-player-live-channel-line-label")) {
                    lineLabelStyleClasses.add("vlc-player-live-channel-line-label");
                }
            }
        }
    }

    private void showLiveChannelBanner(LiveChannel liveChannel, LiveChannel.Line liveChannelLine) {
        String channelTitle = liveChannel.getTitle();

        liveChannelBanner.setChannelInfo(channelTitle, liveChannelLine.getLogoUrl());
        liveChannelBanner.setCurrentProgram(null, null, null);
        liveChannelBanner.setNextProgram(null, null);
        if (epgServiceUrl != null) {
            fetchAndApplyEpgAsync(channelTitle);
        }
        liveChannelBanner.show();
    }

    private void fetchAndApplyEpgAsync(String channelTitle) {
        AsyncUtil.execute(() -> {
            Epg epg = null;
            LocalDateTime now = LocalDateTime.now();
            LocalTime nowTime;
            String epgStartTimeStr;
            String epgEndTimeStr;
            LocalTime epgStartTime;
            LocalTime epgEndTime;
            MutableTriple<String, String, String> currentProgramTitleAndStartTimeAndEndTimeTriple;
            MutablePair<String, String> nextProgramTitleAndStartTimeAndEndTime;
            Epg.Data epgData;

            try {
                epg = epgFetchingHandler.handle(epgServiceUrl, channelTitle, now.toLocalDate())
                        .get(4, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Platform.runLater(() -> ToastHelper.showException(e));
            } catch (ExecutionException | TimeoutException e) {
                log.warn(
                        "Exception while fetching epg, channelTitle={}, epgServiceUrl={}",
                        channelTitle,
                        epgServiceUrl,
                        e
                );
            }
            if (epg == null) {

                return;
            }
            nowTime = now.toLocalTime();
            currentProgramTitleAndStartTimeAndEndTimeTriple = MutableTriple.of(null, null, null);
            nextProgramTitleAndStartTimeAndEndTime = MutablePair.of(null, null);
            List<Epg.Data> data = epg.getEpgData();
            for (int i = 0; i < data.size(); i++) {
                epgData = data.get(i);
                epgStartTimeStr = epgData.getStart();
                epgEndTimeStr = epgData.getEnd();
                if (epgStartTimeStr == null || epgEndTimeStr == null) {
                    break;
                }
                try {
                    epgStartTime = LocalTime.parse(epgData.getStart(), LOCAL_TIME_FORMATTER);
                    epgEndTime = LocalTime.parse(epgData.getEnd(), LOCAL_TIME_FORMATTER);
                } catch (DateTimeParseException e) {
                    log.warn(
                            "Invalid epg time format, channelTitle={}, epgServiceUrl={}, epgData={}",
                            channelTitle, epgServiceUrl, epgData
                    );
                    break;
                }
                if (!nowTime.isBefore(epgStartTime) && nowTime.isBefore(epgEndTime)) {
                    currentProgramTitleAndStartTimeAndEndTimeTriple.setLeft(epgData.getTitle());
                    currentProgramTitleAndStartTimeAndEndTimeTriple.setMiddle(epgStartTimeStr);
                    currentProgramTitleAndStartTimeAndEndTimeTriple.setRight(epgEndTimeStr);
                    if (i < data.size() - 1) {
                        epgData = data.get(i + 1);
                        nextProgramTitleAndStartTimeAndEndTime.setLeft(epgData.getTitle());
                        nextProgramTitleAndStartTimeAndEndTime.setRight(epgData.getStart());
                    }
                    Platform.runLater(() -> {
                        liveChannelBanner.setCurrentProgram(
                                currentProgramTitleAndStartTimeAndEndTimeTriple.getLeft(),
                                currentProgramTitleAndStartTimeAndEndTimeTriple.getMiddle(),
                                currentProgramTitleAndStartTimeAndEndTimeTriple.getRight()
                        );
                        liveChannelBanner.setNextProgram(
                                nextProgramTitleAndStartTimeAndEndTime.getLeft(),
                                nextProgramTitleAndStartTimeAndEndTime.getRight()
                        );
                    });
                    break;
                }
            }
        });
    }

    /**
     * 播放器配置
     */
    @Data
    @Builder
    public static class Config {

        /**
         * 直播模式
         */
        private Boolean liveMode;
    }

    private static class LiveChannelBanner extends HBox {

        private final LogoPlaceholder logoPlaceHolder = new LogoPlaceholder(60, 24);
        private final ImageView logoView = new ImageView();
        private final Label channelNameLabel = new Label();
        private final Label currentProgramLabel = new Label();
        private final Label programTimeLabel = new Label();
        private final Label nextProgramLabel = new Label();
        private final Label nextProgramTimeLabel = new Label();
        private final Map<String, Image> logoUrlAndLogoImage = new HashMap<>();
        private final Timer hideTimer = new Timer(6000, evt -> setVisible(false));

        public LiveChannelBanner() {
            super();

            Rectangle clip;
            HBox epgInfoHBox;
            VBox currentProgramVBox;
            VBox nextProgramVBox;
            VBox programInfoVBox;

            // 基本样式设置
            setAlignment(Pos.BOTTOM_CENTER);
            getStyleClass().add("vlc-player-live-channel-banner");
            setupLogoContainer();

            // 创建台标容器
            clip = new Rectangle(60, 60);
            logoView.setClip(clip);
            logoView.setFitWidth(60);
            logoView.setFitHeight(60);
            logoView.setPreserveRatio(true);

            // 样式
            channelNameLabel.getStyleClass().add("vlc-player-live-channel-banner-channel-name-label");
            currentProgramLabel.getStyleClass().add("vlc-player-live-channel-banner-program-label");
            programTimeLabel.getStyleClass().add("vlc-player-live-channel-banner-program-time-label");
            nextProgramLabel.getStyleClass().add("vlc-player-live-channel-banner-program-label");
            nextProgramTimeLabel.getStyleClass().add("vlc-player-live-channel-banner-program-time-label");

            // 节目信息垂直布局
            currentProgramVBox = new VBox(2, currentProgramLabel, programTimeLabel);
            nextProgramVBox = new VBox(2, nextProgramLabel, nextProgramTimeLabel);
            epgInfoHBox = new HBox(15, currentProgramVBox, nextProgramVBox);
            programInfoVBox = new VBox(5, channelNameLabel, epgInfoHBox);
            programInfoVBox.setAlignment(Pos.CENTER_LEFT);

            // 主布局组装
            getChildren().addAll(logoPlaceHolder, new StackPane(logoView), programInfoVBox);
        }

        private void setupLogoContainer() {
            showLogoPlaceholder(false);
        }

        public void show() {
            setVisible(true);
            hideTimer.restart();
        }

        /**
         * 设置频道信息
         * @param name 名称
         * @param logoUrl LOGO地址
         */
        @SuppressWarnings("ConstantConditions")
        public void setChannelInfo(String name, @Nullable String logoUrl) {
            Image logo;

            channelNameLabel.setText(name);
            if (ValidationUtil.isURL(logoUrl)) {
                if (logoUrlAndLogoImage.containsKey(logoUrl)) {
                    logoView.setImage(logoUrlAndLogoImage.get(logoUrl));
                } else {
                    logoPlaceHolder.setPlaceholderText(name.substring(0, 1));
                    showLogoPlaceholder(true);
                    logo = new Image(logoUrl, true);
                    logo.progressProperty()
                            .addListener((ob, oldVal, newVal) -> {
                                if (newVal.doubleValue() >= 1.0 && !logo.isError()) {
                                    logoUrlAndLogoImage.put(logoUrl, logo);
                                    logoView.setImage(logo);
                                    showLogoPlaceholder(false);
                                }
                            });
                }
            } else {
                logoPlaceHolder.setPlaceholderText(name.substring(0, 1));
                showLogoPlaceholder(true);
            }
        }

        /**
         * 设置是否展示默认LOGO
         * @param show 是否展示
         */
        public void showLogoPlaceholder(boolean show) {
            logoPlaceHolder.setVisible(show);
            logoPlaceHolder.setManaged(show);
            logoView.setVisible(!show);
            logoView.setManaged(!show);
        }

        /**
         * 设置当前节目信息
         * @param title 标题
         * @param startTime 开始时间
         * @param endTime 结束时间
         */
        public void setCurrentProgram(@Nullable String title, @Nullable String startTime, @Nullable String endTime) {
            if (StringUtils.isBlank(title)) {
                title = I18nHelper.get(I18nKeys.LIVE_PLAYER_DEFAULT_PROGRAM);
            }
            if (StringUtils.isBlank(startTime)) {
                startTime = "--:--";
            }
            if (StringUtils.isBlank(endTime)) {
                endTime = "--:--";
            }
            programTimeLabel.setText(I18nHelper.getFormatted(
                    I18nKeys.LIVE_PLAYER_PLAY_TIME, startTime + " - " + endTime
            ));
            currentProgramLabel.setText(I18nHelper.getFormatted(I18nKeys.LIVE_PLAYER_CURRENT_PROGRAM, title));
        }

        /**
         * 设置下个节目信息
         * @param title 标题
         * @param startTime 开始时间
         */
        public void setNextProgram(@Nullable String title, @Nullable String startTime) {
            if (StringUtils.isBlank(title)) {
                title = I18nHelper.get(I18nKeys.LIVE_PLAYER_DEFAULT_PROGRAM);
            }
            if (StringUtils.isBlank(startTime)) {
                startTime = "--:--";
            }
            nextProgramLabel.setText(I18nHelper.getFormatted(I18nKeys.LIVE_PLAYER_NEXT_PROGRAM, title));
            nextProgramTimeLabel.setText(I18nHelper.getFormatted(I18nKeys.LIVE_PLAYER_START_TIME, startTime));
        }
    }

    private static class LogoPlaceholder extends StackPane {

        private final Label placeholderLabel;
        private final static List<Pair<Paint, Paint>> BACKGROUND_TEXT_PAINT_PAIRS = List.of(
                Pair.of(Color.rgb(70, 130, 180), Color.WHITE),
                Pair.of(Color.rgb(255, 127, 80), Color.WHITE),
                Pair.of(Color.rgb(39, 139, 34), Color.WHITE),
                Pair.of(Color.rgb(200, 160, 0), Color.WHITE),
                Pair.of(Color.rgb(85, 107, 47), Color.WHITE),
                Pair.of(Color.rgb(255, 140, 0), Color.WHITE),
                Pair.of(Color.rgb(188, 10, 40), Color.WHITE),
                Pair.of(Pair.of(Color.rgb(138, 43, 226), Color.WHITE)),
                Pair.of(Color.rgb(65, 105, 225), Color.WHITE),
                Pair.of(Color.rgb(255, 105, 180), Color.WHITE),
                Pair.of(Color.rgb(210, 105, 30), Color.WHITE),
                Pair.of(Color.rgb(0, 139, 139), Color.WHITE)
        );

        public LogoPlaceholder(double size, double fontSize) {
            super();
            Rectangle background = new Rectangle(size, size);
            int colorPairIdx = RandomUtils.nextInt(0, BACKGROUND_TEXT_PAINT_PAIRS.size() - 1);
            Pair<Paint, Paint> colorPair = BACKGROUND_TEXT_PAINT_PAIRS.get(colorPairIdx);

            placeholderLabel = new Label();
            placeholderLabel.getStyleClass().add("vlc-player-live-channel-banner-logo-placeholder-label");
            placeholderLabel.setFont(Font.font(null, FontWeight.BOLD, fontSize));
            placeholderLabel.setTextFill(colorPair.getRight());
            this.setPrefSize(size, size);
            this.setMinSize(size, size);
            background.setFill(colorPair.getLeft());
            background.setArcWidth(20);
            background.setArcHeight(20);
            this.getChildren().addAll(background, placeholderLabel);
        }

        public void setPlaceholderText(String text) {
            placeholderLabel.setText(text);
        }
    }

    private static class LiveDrawer extends HBox {

        private final ListView<LiveChannelGroup> liveChannelGroupListView;
        private final LiveChannelGroupListViewCellFactory liveChannelGroupListViewCellFactory;
        private final ListView<LiveChannel> liveChannelListView;
        private final LiveChannelListViewCellFactory liveChannelListViewCellFactory;

        public LiveDrawer(LiveInfoBO selectedLive, LiveInfoBO playingLive, StackPane playerPane, VLCPlayer player) {
            super();
            this.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
            HBox listViewHBox = new HBox();
            Button actionBtn = new Button(">");
            DoubleProperty minHeightProp = minHeightProperty();
            DoubleProperty maxHeightProp = maxHeightProperty();
            DoubleProperty minWidthProp = minWidthProperty();
            DoubleProperty maxWidthProp = maxWidthProperty();
            ReadOnlyDoubleProperty playerPaneHeightProp = playerPane.heightProperty();
            ReadOnlyDoubleProperty playerPaneWidthProp = playerPane.widthProperty();
            ReadOnlyDoubleProperty listViewHBoxWidthProp = listViewHBox.widthProperty();
            ReadOnlyDoubleProperty actionBtnWidthProp = actionBtn.widthProperty();
            ObservableList<Node> liveDrawerChildren = getChildren();
            ObservableList<Node> listViewHBoxChildren = listViewHBox.getChildren();

            // 样式
            minHeightProp.bind(playerPaneHeightProp.divide(1.2));
            maxHeightProp.bind(playerPaneHeightProp.divide(1.2));
            minWidthProp.bind(actionBtnWidthProp);
            maxWidthProp.bind(actionBtnWidthProp);
            listViewHBox.setManaged(false);
            listViewHBox.setVisible(false);
            listViewHBox.minWidthProperty().bind(playerPaneWidthProp.divide(2.8));
            listViewHBox.maxWidthProperty().bind(playerPaneWidthProp.divide(2.8));
            liveDrawerChildren.add(listViewHBox);
            liveDrawerChildren.add(new StackPane(actionBtn));
            // 节目列表
            liveChannelListView = new ListView<>();
            liveChannelListViewCellFactory = new LiveChannelListViewCellFactory(selectedLive, playingLive, player);
            liveChannelListView.setCellFactory(liveChannelListViewCellFactory);
            liveChannelListView.getStyleClass().add("vlc-player-live-channel-list-view");
            liveChannelListView.setFocusTraversable(false);
            HBox.setHgrow(liveChannelListView, Priority.ALWAYS);
            // 节目分组列表
            liveChannelGroupListView = new ListView<>();
            liveChannelGroupListViewCellFactory = new LiveChannelGroupListViewCellFactory(
                    selectedLive, liveChannelListView, liveChannelListViewCellFactory.getLiveChannelAndTitleHBoxMap()
            );
            liveChannelGroupListView.setCellFactory(liveChannelGroupListViewCellFactory);
            liveChannelGroupListView.getStyleClass().add("vlc-player-live-channel-list-view");
            liveChannelGroupListView.setFocusTraversable(false);

            listViewHBoxChildren.add(liveChannelGroupListView);
            listViewHBoxChildren.add(liveChannelListView);

            // 展开/收起 按钮
            actionBtn.setFocusTraversable(false);
            actionBtn.setOnMouseClicked(evt -> {
                listViewHBox.setManaged(!listViewHBox.isManaged());
                listViewHBox.setVisible(!listViewHBox.isVisible());
                // 根据显示状态，更新Drawer的宽度
                minWidthProp.unbind();
                maxWidthProp.unbind();
                if (listViewHBox.isVisible()) {
                    minWidthProp.bind(listViewHBoxWidthProp.add(actionBtnWidthProp));
                    maxWidthProp.bind(listViewHBoxWidthProp.add(actionBtnWidthProp));
                    actionBtn.setText("<");
                } else {
                    minWidthProp.bind(actionBtnWidthProp);
                    maxWidthProp.bind(actionBtnWidthProp);
                    actionBtn.setText(">");
                }
            });
            actionBtn.setMinWidth(30);
            actionBtn.minHeightProperty().bind(actionBtn.widthProperty().multiply(2));
        }

        public void setLiveChannelGroups(List<LiveChannelGroup> liveChannelGroups) {
            ObservableList<LiveChannelGroup> items = liveChannelGroupListView.getItems();

            items.clear();
            items.addAll(liveChannelGroups);
        }

        public void select(LiveChannelGroup liveChannelGroup, LiveChannel liveChannel) {
            liveChannelGroupListViewCellFactory.select(liveChannelGroup, true);
            liveChannelListViewCellFactory.select(liveChannel);
        }
    }

    private static class LiveChannelGroupListViewCellFactory
            implements Callback<ListView<LiveChannelGroup>, ListCell<LiveChannelGroup>>
    {
        private final LiveInfoBO selectedLive;
        private final ListView<LiveChannel> liveChannelListView;
        private final Map<LiveChannelGroup, Label> liveChannelGroupAndTitleLabelMap;
        private final Map<LiveChannel, HBox> liveChannelAndTitleHBoxMap;

        public LiveChannelGroupListViewCellFactory(
                LiveInfoBO selectedLive,
                ListView<LiveChannel> liveChannelListView,
                Map<LiveChannel, HBox> liveChannelAndTitleHBoxMap
        ) {
            super();
            this.selectedLive = selectedLive;
            this.liveChannelListView = liveChannelListView;
            this.liveChannelAndTitleHBoxMap = liveChannelAndTitleHBoxMap;
            this.liveChannelGroupAndTitleLabelMap = new HashMap<>();
        }

        @Override
        public ListCell<LiveChannelGroup> call(ListView<LiveChannelGroup> liveChannelGroupListView) {
            return new ListCell<>() {

                @Override
                protected void updateItem(LiveChannelGroup liveChannelGroup, boolean empty) {
                    List<String> styleClasses;
                    Label titleLabel;
                    List<String> titleLabelStyleClass;
                    LiveChannelGroup selectedLiveChannelGroup;

                    super.updateItem(liveChannelGroup, empty);
                    styleClasses = getStyleClass();
                    if (!styleClasses.contains("vlc-player-live-channel-group-list-cell")) {
                        styleClasses.add("vlc-player-live-channel-group-list-cell");
                    }
                    setText(null);
                    if (empty) {
                        setGraphic(null);

                        return;
                    }
                    titleLabel = liveChannelGroupAndTitleLabelMap.get(liveChannelGroup);
                    if (titleLabel == null) {
                        titleLabel = new Label(liveChannelGroup.getTitle());
                        titleLabel.setAlignment(Pos.CENTER);
                        titleLabelStyleClass = titleLabel.getStyleClass();
                        titleLabel.maxWidthProperty().bind(liveChannelGroupListView.widthProperty().divide(1.3));
                        titleLabel.setOnMouseClicked(evt -> {
                            if (evt.getButton() != MouseButton.PRIMARY) {

                                return;
                            }
                            select(liveChannelGroup, false);
                        });
                        liveChannelGroupAndTitleLabelMap.put(liveChannelGroup, titleLabel);
                        selectedLiveChannelGroup = selectedLive.getLiveChannelGroup();
                        if (selectedLiveChannelGroup != null && liveChannelGroup == selectedLiveChannelGroup) {
                            titleLabelStyleClass.add("vlc-player-live-channel-list-view-title-label-focused");
                            setLiveChannels(liveChannelGroup.getChannels(), false);
                        } else {
                            titleLabelStyleClass.add("vlc-player-live-channel-list-view-title-label");
                        }
                    }
                    setGraphic(titleLabel);
                    setAlignment(Pos.CENTER);
                }
            };
        }

        /**
         * 选中节目分组
         * @param liveChannelGroup 被选中的节目分组
         * @param isPlaying 是否是播放选中。即是否为“上一集/下一集”切换时的选中（如果是，需要移除节目列表中上一个的节目标题高亮）
         */
        private void select(LiveChannelGroup liveChannelGroup, boolean isPlaying) {
            Label titleLabel = liveChannelGroupAndTitleLabelMap.get(liveChannelGroup);
            LiveChannelGroup lastLiveChannelGroup;
            List<String> titleLabelStyleClass;
            Label lastSelectedTitleLabel;
            List<String> lastSelectedTitleLabelStyleClass;

            lastLiveChannelGroup = selectedLive.getLiveChannelGroup();
            selectedLive.setLiveChannelGroup(liveChannelGroup);
            if (titleLabel == null) {

                return;
            }
            titleLabelStyleClass = titleLabel.getStyleClass();
            if (lastLiveChannelGroup == null) {
                titleLabelStyleClass.remove("vlc-player-live-channel-list-view-title-label");
                titleLabelStyleClass.add("vlc-player-live-channel-list-view-title-label-focused");
                setLiveChannels(liveChannelGroup.getChannels(), false);
            } else if (lastLiveChannelGroup != liveChannelGroup) {
                titleLabelStyleClass.remove("vlc-player-live-channel-list-view-title-label");
                titleLabelStyleClass.add("vlc-player-live-channel-list-view-title-label-focused");
                lastSelectedTitleLabel = liveChannelGroupAndTitleLabelMap.get(lastLiveChannelGroup);
                lastSelectedTitleLabelStyleClass = lastSelectedTitleLabel.getStyleClass();
                lastSelectedTitleLabelStyleClass.remove(
                        "vlc-player-live-channel-list-view-title-label-focused"
                );
                lastSelectedTitleLabelStyleClass.add(
                        "vlc-player-live-channel-list-view-title-label"
                );
                setLiveChannels(liveChannelGroup.getChannels(), isPlaying);
            }
        }

        /**
         * 设置节目列表
         * @param liveChannels 节目列表
         * @param clearLastHighlightStyle 是否移除上一个被选中的节目的高亮效果
         */
        private void setLiveChannels(List<LiveChannel> liveChannels, boolean clearLastHighlightStyle) {
            ObservableList<LiveChannel> items = liveChannelListView.getItems();
            LiveChannel lastSelectedLiveChannel;
            HBox lastLiveChannelTitleHBox;
            Label lastLiveChannelTitleLabel;
            List<String> lastLiveChannelTitleLabelStyleClass;

            items.clear();
            items.addAll(liveChannels);
            if (!clearLastHighlightStyle) {

                return;
            }
            lastSelectedLiveChannel = selectedLive.getLiveChannel();
            if (lastSelectedLiveChannel == null) {

                return;
            }
            lastLiveChannelTitleHBox = liveChannelAndTitleHBoxMap.get(lastSelectedLiveChannel);
            if (lastLiveChannelTitleHBox == null) {

                return;
            }
            lastLiveChannelTitleLabel = CastUtil.cast(CollectionUtil.getFirst(lastLiveChannelTitleHBox.getChildren()));
            if (lastLiveChannelTitleLabel == null) {

                return;
            }
            lastLiveChannelTitleLabelStyleClass = lastLiveChannelTitleLabel.getStyleClass();
            lastLiveChannelTitleLabelStyleClass.remove("vlc-player-live-channel-list-view-title-label-focused");
            lastLiveChannelTitleLabelStyleClass.add("vlc-player-live-channel-list-view-title-label");
        }
    }

    private static class LiveChannelListViewCellFactory
            implements Callback<ListView<LiveChannel>, ListCell<LiveChannel>>
    {
        private final LiveInfoBO selectedLive;
        private final LiveInfoBO playingLive;
        private final VLCPlayer player;
        private final Map<LiveChannel, BorderPane> liveChannelAndGraphicBorderPaneMap;
        @Getter
        private final Map<LiveChannel, HBox> liveChannelAndTitleHBoxMap;

        private final static double LOGO_IMAGE_SIZE = 50;
        private final static double LOGO_PLACE_HOLDER_SIZE = 30;
        private final static double LOGO_PLACE_HOLDER_FONT_SIZE = 18;
        private final static ImageView PLAYING_GIF_IMAGE_VIEW;

        static {
            PLAYING_GIF_IMAGE_VIEW = new ImageView(BaseResources.PLAYING_GIF);
            PLAYING_GIF_IMAGE_VIEW.setFitWidth(LOGO_PLACE_HOLDER_SIZE);
            PLAYING_GIF_IMAGE_VIEW.setFitHeight(LOGO_PLACE_HOLDER_SIZE);
            PLAYING_GIF_IMAGE_VIEW.setPreserveRatio(true);
        }

        public LiveChannelListViewCellFactory(LiveInfoBO selectedLive, LiveInfoBO playingLive, VLCPlayer player) {
            super();
            this.selectedLive = selectedLive;
            this.playingLive = playingLive;
            this.player = player;
            this.liveChannelAndGraphicBorderPaneMap = new HashMap<>();
            this.liveChannelAndTitleHBoxMap = new HashMap<>();
        }

        @Override
        @SuppressWarnings("ConstantConditions")
        public ListCell<LiveChannel> call(ListView<LiveChannel> liveChannelListView) {
            return new ListCell<>() {
                @Override
                protected void updateItem(LiveChannel liveChannel, boolean empty) {
                    List<String> styleClasses;
                    BorderPane graphicBorderPane;
                    Label titleLabel;
                    List<String> titleLabelStyleClass;
                    HBox titleHBox;
                    LiveChannel selectedLiveChannel;
                    List<Node> titleHBoxChildren;
                    String logoUrl;
                    LogoPlaceholder logoPlaceholder;
                    Image logo;
                    ImageView logoImageView;

                    super.updateItem(liveChannel, empty);
                    styleClasses = getStyleClass();
                    if (!styleClasses.contains("vlc-player-live-channel-list-cell")) {
                        styleClasses.add("vlc-player-live-channel-list-cell");
                    }
                    setText(null);
                    if (empty) {
                        setGraphic(null);

                        return;
                    }
                    graphicBorderPane = ObjectUtil.defaultIfNull(
                            liveChannelAndGraphicBorderPaneMap.get(liveChannel), () -> new BorderPane()
                    );
                    if (graphicBorderPane.getCenter() == null) {
                        // 初始化Graphic
                        liveChannelAndGraphicBorderPaneMap.put(liveChannel, graphicBorderPane);
                        titleLabel = new Label(liveChannel.getTitle());
                        titleLabelStyleClass = titleLabel.getStyleClass();
                        titleLabel.maxWidthProperty().bind(liveChannelListView.widthProperty().divide(1.3));
                        titleLabel.setOnMousePressed(evt -> {
                            LiveChannelGroup liveChannelGroup;

                            if (evt.getButton() != MouseButton.PRIMARY || playingLive.getLiveChannel() == liveChannel) {

                                return;
                            }
                            liveChannelGroup = selectedLive.getLiveChannelGroup();
                            player.play(liveChannelGroup, liveChannel, liveChannel.getLines().get(0));
                        });
                        titleHBox = new HBox(titleLabel);
                        liveChannelAndTitleHBoxMap.put(liveChannel, titleHBox);
                        selectedLiveChannel = selectedLive.getLiveChannel();
                        if (selectedLiveChannel != null && liveChannel == selectedLiveChannel) {
                            titleLabelStyleClass.add("vlc-player-live-channel-list-view-title-label-focused");
                            titleHBoxChildren = titleHBox.getChildren();
                            if (!titleHBoxChildren.contains(PLAYING_GIF_IMAGE_VIEW)) {
                                titleHBoxChildren.add(1, PLAYING_GIF_IMAGE_VIEW);
                            }
                        } else {
                            titleLabelStyleClass.add("vlc-player-live-channel-list-view-title-label");
                        }
                        graphicBorderPane.setCenter(titleHBox);
                        // 台标
                        logoUrl = liveChannel.getLines()
                                .stream()
                                .map(LiveChannel.Line::getLogoUrl)
                                .filter(StringUtils::isNotBlank)
                                .findFirst()
                                .orElse(null);
                        // 默认台标
                        logoPlaceholder = new LogoPlaceholder(LOGO_PLACE_HOLDER_SIZE, LOGO_PLACE_HOLDER_FONT_SIZE);
                        logoPlaceholder.setPlaceholderText(liveChannel.getTitle().substring(0, 1));
                        graphicBorderPane.setLeft(logoPlaceholder);
                        if (ValidationUtil.isURL(logoUrl)) {
                            // 后台加载台标
                            logo = new Image(logoUrl, true);
                            logoImageView = new ImageView(logo);
                            logoImageView.setFitWidth(LOGO_IMAGE_SIZE);
                            logoImageView.setFitHeight(LOGO_IMAGE_SIZE);
                            logoImageView.setPreserveRatio(true);
                            logo.progressProperty()
                                    .addListener((ob, oldVal, newVal) -> {
                                        if (newVal.doubleValue() >= 1 && !logo.isError()) {
                                            graphicBorderPane.setLeft(logoImageView);
                                        }
                                    });
                        }
                    }
                    setGraphic(graphicBorderPane);
                    setAlignment(Pos.CENTER);
                }
            };
        }

        private void select(LiveChannel liveChannel) {
            HBox titleHBox = liveChannelAndTitleHBoxMap.get(liveChannel);
            Label titleLabel;
            LiveChannel lastLiveChannel;
            List<String> titleLabelStyleClass;
            List<Node> titleHBoxChildren;
            HBox lastSelectedTitleHBox;
            Label lastSelectedTitleLabel;
            List<String> lastSelectedTitleLabelStyleClass;
            List<Node> lastSelectedTitleHBoxChildren;

            lastLiveChannel = selectedLive.getLiveChannel();
            selectedLive.setLiveChannel(liveChannel);
            if (titleHBox == null) {

                return;
            }
            titleLabel = CastUtil.cast(CollectionUtil.getFirst(titleHBox.getChildren()));
            if (titleLabel == null) {

                return;
            }
            titleLabelStyleClass = titleLabel.getStyleClass();
            titleHBoxChildren = titleHBox.getChildren();
            if (lastLiveChannel == null) {
                titleLabelStyleClass.remove("vlc-player-live-channel-list-view-title-label");
                titleLabelStyleClass.add("vlc-player-live-channel-list-view-title-label-focused");
                if (!titleHBoxChildren.contains(PLAYING_GIF_IMAGE_VIEW)) {
                    titleHBoxChildren.add(1, PLAYING_GIF_IMAGE_VIEW);
                }
            } else if (lastLiveChannel != liveChannel) {
                titleLabelStyleClass.remove("vlc-player-live-channel-list-view-title-label");
                titleLabelStyleClass.add("vlc-player-live-channel-list-view-title-label-focused");
                lastSelectedTitleHBox = liveChannelAndTitleHBoxMap.get(lastLiveChannel);
                lastSelectedTitleLabel = CastUtil.cast(CollectionUtil.getFirst(lastSelectedTitleHBox.getChildren()));
                if (lastSelectedTitleLabel == null) {

                    return;
                }
                lastSelectedTitleLabelStyleClass = lastSelectedTitleLabel.getStyleClass();
                lastSelectedTitleLabelStyleClass.remove(
                        "vlc-player-live-channel-list-view-title-label-focused"
                );
                lastSelectedTitleLabelStyleClass.add(
                        "vlc-player-live-channel-list-view-title-label"
                );
                lastSelectedTitleHBoxChildren = lastSelectedTitleHBox.getChildren();
                lastSelectedTitleHBoxChildren.remove(PLAYING_GIF_IMAGE_VIEW);
                if (!titleHBoxChildren.contains(PLAYING_GIF_IMAGE_VIEW)) {
                    titleHBoxChildren.add(1, PLAYING_GIF_IMAGE_VIEW);
                }
            }
        }
    }

    @Data
    private static class LiveInfoBO {

        private LiveChannelGroup liveChannelGroup;

        private LiveChannel liveChannel;

        private LiveChannel.Line liveChannelLine;
    }
}
