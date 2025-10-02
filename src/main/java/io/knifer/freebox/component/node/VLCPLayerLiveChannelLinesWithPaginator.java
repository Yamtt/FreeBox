package io.knifer.freebox.component.node;

import io.knifer.freebox.model.domain.LiveChannel;
import io.knifer.freebox.util.CollectionUtil;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 播放器（直播模式） - 频道线路分页
 *
 * @author Knifer
 */
public class VLCPLayerLiveChannelLinesWithPaginator extends HBox {

    private LiveChannel.Line playingLiveChannelLine;

    private final SimpleIntegerProperty currentPage = new SimpleIntegerProperty(-1);
    private final SimpleIntegerProperty totalPage = new SimpleIntegerProperty(-1);
    private final HBox linesHBox = new HBox(8);
    private final Consumer<LiveChannel.Line> onLiveChannelLineChanged;
    private final List<VLCPlayerLiveChannelLineLabel> lineLabels;

    private final static int PAGE_SIZE = 5;

    public VLCPLayerLiveChannelLinesWithPaginator(Consumer<LiveChannel.Line> onLiveChannelLineChanged) {
        super(8);

        Label previousLabel = new Label();
        Label nextLabel = new Label();
        List<Node> children;

        this.onLiveChannelLineChanged = onLiveChannelLineChanged;
        this.lineLabels = new ArrayList<>();
        previousLabel.setGraphic(FontIcon.of(FontAwesome.CARET_LEFT, 32, Color.WHITE));
        previousLabel.setCursor(Cursor.HAND);
        nextLabel.setGraphic(FontIcon.of(FontAwesome.CARET_RIGHT, 32, Color.WHITE));
        nextLabel.setCursor(Cursor.HAND);
        previousLabel.visibleProperty().bind(currentPage.greaterThan(1));
        nextLabel.visibleProperty().bind(currentPage.lessThan(totalPage));
        previousLabel.setOnMouseClicked(evt -> {
            int nowCurrentPage;
            List<Node> linesHBoxChildren;

            if (evt.getButton() != MouseButton.PRIMARY || currentPage.get() <= 1) {

                return;
            }
            nowCurrentPage = currentPage.get() - 1;
            currentPage.set(nowCurrentPage);
            linesHBoxChildren = linesHBox.getChildren();
            linesHBoxChildren.clear();
            for (int i = (nowCurrentPage - 1) * PAGE_SIZE; i < nowCurrentPage * PAGE_SIZE; i++) {
                if (i >= lineLabels.size()) {
                    break;
                }
                linesHBoxChildren.add(lineLabels.get(i));
            }
        });
        nextLabel.setOnMouseClicked(evt -> {
            int nowCurrentPage;
            List<Node> linesHBoxChildren;

            if (evt.getButton() != MouseButton.PRIMARY || currentPage.get() >= totalPage.get()) {

                return;
            }
            nowCurrentPage = currentPage.get() + 1;
            currentPage.set(nowCurrentPage);
            linesHBoxChildren = linesHBox.getChildren();
            linesHBoxChildren.clear();
            for (int i = (nowCurrentPage - 1) * PAGE_SIZE; i < nowCurrentPage * PAGE_SIZE; i++) {
                if (i >= lineLabels.size()) {
                    break;
                }
                linesHBoxChildren.add(lineLabels.get(i));
            }
        });
        children = getChildren();
        children.add(previousLabel);
        children.add(linesHBox);
        children.add(nextLabel);
    }

    public void clear() {
        linesHBox.getChildren().clear();
        lineLabels.clear();
        currentPage.set(-1);
        totalPage.set(-1);
        playingLiveChannelLine = null;
    }

    public void addLine(LiveChannel.Line line) {
        VLCPlayerLiveChannelLineLabel newLineLabel = new VLCPlayerLiveChannelLineLabel(line);
        ObservableList<Node> linesHBoxChildren = linesHBox.getChildren();
        Node firstLineLabel;
        List<String> firstLineLabelStyleClasses;

        newLineLabel.setOnMouseClicked(evt -> {
            List<String> lineLabelStyleClasses;

            if (evt.getButton() != MouseButton.PRIMARY || line == playingLiveChannelLine) {

                return;
            }
            for (VLCPlayerLiveChannelLineLabel lineLabel : lineLabels) {
                lineLabelStyleClasses = lineLabel.getStyleClass();
                if (playingLiveChannelLine == null || lineLabel.getLiveChannelLine() == line) {
                    // 为正在播放的线路标签添加样式
                    lineLabelStyleClasses.remove("vlc-player-live-channel-line-label");
                    if (!lineLabelStyleClasses.contains("vlc-player-live-channel-line-label-focused")) {
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
            playingLiveChannelLine = line;
            onLiveChannelLineChanged.accept(line);
        });
        lineLabels.add(newLineLabel);
        if (currentPage.get() == -1) {
            currentPage.set(1);
        }
        if (totalPage.get() == -1) {
            totalPage.set(1);
        } else {
            totalPage.set((lineLabels.size() - 1) / PAGE_SIZE + 1);
        }
        if (lineLabels.size() <= PAGE_SIZE) {
            linesHBoxChildren.add(newLineLabel);
        }
        // 默认将第一个线路标签设为正在播放
        firstLineLabel = CollectionUtil.getFirst(linesHBoxChildren);
        if (firstLineLabel != null && playingLiveChannelLine == null) {
            firstLineLabelStyleClasses = firstLineLabel.getStyleClass();
            firstLineLabelStyleClasses.remove("vlc-player-live-channel-line-label");
            if (!firstLineLabelStyleClasses.contains("vlc-player-live-channel-line-label-focused")) {
                firstLineLabelStyleClasses.add("vlc-player-live-channel-line-label-focused");
            }
            playingLiveChannelLine = ((VLCPlayerLiveChannelLineLabel) firstLineLabel).getLiveChannelLine();
        }
    }
}
