package io.knifer.freebox.component.factory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.model.common.tvbox.Movie;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.ValidationUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.controlsfx.control.InfoOverlay;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 影片单元格工厂
 *
 * @author Knifer
 */
@Slf4j
public class VideoGridCellFactory implements Callback<GridView<Movie.Video>, GridCell<Movie.Video>> {

    private final Consumer<Movie.Video> onVideoOpen;

    private final Consumer<VideoGridCell> onLoadMoreMovie;

    private final BooleanProperty showSourceName = new SimpleBooleanProperty(false);

    private static final Map<String, String> SOURCE_KEY_AND_NAME_MAP = new HashMap<>();

    private static final Cache<String, Image> PICTURE_CACHE = CacheBuilder.newBuilder()
            .maximumSize(150)
            .build();

    private static final Set<String> LOADED_PICTURES = new HashSet<>();

    private static final Map<String, StackPane> ITEM_ID_AND_CONTAINER_MAP = new HashMap<>();

    private static final double CELL_WIDTH = 150;

    private static final double CELL_HEIGHT = 200;

    public VideoGridCellFactory(Consumer<Movie.Video> onVideoOpen, Consumer<VideoGridCell> onLoadMoreMovie) {
        this.onVideoOpen = onVideoOpen;
        this.onLoadMoreMovie = onLoadMoreMovie;
    }

    public void setSourceBeans(Collection<SourceBean> sourceBeans) {
        if (CollectionUtil.isEmpty(sourceBeans)) {

            return;
        }
        SOURCE_KEY_AND_NAME_MAP.clear();
        SOURCE_KEY_AND_NAME_MAP.putAll(
                sourceBeans.stream().collect(Collectors.toUnmodifiableMap(
                        SourceBean::getKey, SourceBean::getName, (v1, v2) -> v2
                ))
        );
    }

    public void setShowSourceName(boolean flag) {
        showSourceName.set(flag);
    }

    @Override
    public GridCell<Movie.Video> call(GridView<Movie.Video> view) {
        VideoGridCell cell = new VideoGridCell(onVideoOpen, onLoadMoreMovie, showSourceName);

        cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        return cell;
    }

    @RequiredArgsConstructor
    public static class VideoGridCell extends GridCell<Movie.Video> {

        private EventHandler<MouseEvent> eventFilter = null;
        private final Consumer<Movie.Video> onVideoOpen;
        private final Consumer<VideoGridCell> onLoadMoreMovie;
        private final BooleanProperty showSourceName;

        @Override
        protected void updateItem(Movie.Video item, boolean empty) {
            Node rootNode;
            StackPane root;
            StackPane container;
            List<Node> rootChildren;
            String itemId;
            List<Node> containerChildren;
            InfoOverlay movieInfoOverlay;
            Label movieNoteLabel;
            ImageView moviePicImageView;
            Image moviePicImage;
            Image newMoviePicImage;
            String note;
            String picUrl;
            String sourceName;
            Label sourceNameLabel;
            StackPane sourceNameContainer;

            super.updateItem(item, empty);
            rootNode = getGraphic();
            if (rootNode == null) {
                // 初始化根结点
                root = new StackPane();
                setGraphic(root);
            } else {
                // 获取到了根结点，无需初始化
                root = (StackPane) rootNode;
            }
            if (item == null || empty) {

                return;
            }
            itemId = item.getId();
            container = ITEM_ID_AND_CONTAINER_MAP.get(itemId);
            if (container == null) {
                // 缓存中找不到container结点，进行初始化
                container = new StackPane();
                container.setAlignment(Pos.TOP_RIGHT);
                containerChildren = container.getChildren();
                // 影片左上角源名称
                sourceNameLabel = new Label();
                sourceNameLabel.getStyleClass().add("movie-source-label");
                sourceName = SOURCE_KEY_AND_NAME_MAP.get(item.getSourceKey());
                if (StringUtils.isNotBlank(sourceName)) {
                    sourceNameLabel.setText(sourceName);
                }
                sourceNameContainer = new StackPane(sourceNameLabel);
                sourceNameContainer.setAlignment(Pos.TOP_LEFT);
                sourceNameContainer.visibleProperty().bind(showSourceName);
                // 图片
                moviePicImageView = new ImageView();
                if (BaseValues.LOAD_MORE_ITEM_ID.equals(itemId)) {
                    moviePicImageView.setImage(BaseResources.LOAD_MORE_IMG);
                } else {
                    if (itemId == null) {
                        moviePicImageView.setImage(BaseResources.PICTURE_PLACEHOLDER_IMG);
                    } else if ((moviePicImage = PICTURE_CACHE.getIfPresent(itemId)) == null) {
                        moviePicImageView.setImage(BaseResources.PICTURE_PLACEHOLDER_IMG);
                        picUrl = item.getPic();
                        if (!LOADED_PICTURES.contains(picUrl) && ValidationUtil.isURL(picUrl)) {
                            LOADED_PICTURES.add(picUrl);
                            newMoviePicImage = new Image(picUrl, true);
                            newMoviePicImage.progressProperty().addListener(observable -> {
                                if (newMoviePicImage.getProgress() >= 1.0 && !newMoviePicImage.isError()) {
                                    moviePicImageView.setImage(newMoviePicImage);
                                    PICTURE_CACHE.put(itemId, newMoviePicImage);
                                }
                            });
                        }
                    } else {
                        moviePicImageView.setImage(moviePicImage);
                    }
                }
                moviePicImageView.setFitWidth(CELL_WIDTH);
                moviePicImageView.setFitHeight(CELL_HEIGHT);
                movieInfoOverlay = new InfoOverlay(moviePicImageView, item.getName());
                movieInfoOverlay.getStyleClass().add("movie-info-overlay");
                // 影片右上角备注
                note = item.getNote();
                containerChildren.add(movieInfoOverlay);
                containerChildren.add(sourceNameContainer);
                if (StringUtils.isNotBlank(note)) {
                    movieNoteLabel = new Label(note);
                    movieNoteLabel.getStyleClass().add("movie-remark-label");
                    containerChildren.add(movieNoteLabel);
                }
                ITEM_ID_AND_CONTAINER_MAP.put(itemId, container);
            }
            rootChildren = root.getChildren();
            if (rootChildren.contains(container)) {
                // 根结点中已添加了对应的container结点，无需重复添加
                return;
            }
            rootChildren.clear();
            rootChildren.add(container);
            if (eventFilter != null) {
                removeEventFilter(MouseEvent.MOUSE_CLICKED, eventFilter);
            }
            eventFilter = evt -> {
                if (evt.getButton() != MouseButton.PRIMARY) {

                    return;
                }
                if (BaseValues.LOAD_MORE_ITEM_ID.equals(itemId)) {
                    onLoadMoreMovie.accept(this);

                    return;
                }
                if (evt.getClickCount() == 2) {
                    onVideoOpen.accept(item);
                }
            };
            addEventFilter(MouseEvent.MOUSE_CLICKED, eventFilter);
            setId(itemId);
        }
    }

    public void destroy() {
        PICTURE_CACHE.invalidateAll();
        SOURCE_KEY_AND_NAME_MAP.clear();
        LOADED_PICTURES.clear();
        ITEM_ID_AND_CONTAINER_MAP.clear();
    }
}
