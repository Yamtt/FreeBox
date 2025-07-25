package io.knifer.freebox.constant;

import io.knifer.freebox.helper.I18nHelper;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import lombok.experimental.UtilityClass;

/**
 * FreeBox按钮类型
 *
 * @author Knifer
 */
@UtilityClass
public class ButtonTypes {

    public static final ButtonType OK =
            new ButtonType(I18nHelper.get(I18nKeys.COMMON_OK), ButtonBar.ButtonData.OK_DONE);

    public static final ButtonType OPEN_DIRECTLY =
            new ButtonType(I18nHelper.get(I18nKeys.UPGRADE_INSTALL_DIALOG_OPEN_DIRECTLY), ButtonBar.ButtonData.OK_DONE);

    public static final ButtonType OPEN_PATH =
            new ButtonType(I18nHelper.get(I18nKeys.UPGRADE_INSTALL_DIALOG_OPEN_PATH), ButtonBar.ButtonData.OK_DONE);
}
