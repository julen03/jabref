package org.jabref.gui.help;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.Version;

public class NewVersionDialog extends BaseDialog<Boolean> {

    public NewVersionDialog(Version currentVersion,
                            Version latestVersion,
                            DialogService dialogService,
                            ExternalApplicationsPreferences externalApplicationsPreferences) {
        this.setTitle(Localization.lang("New version available"));

        ButtonType btnIgnoreUpdate = new ButtonType(Localization.lang("Ignore this update"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType btnDownloadUpdate = new ButtonType(Localization.lang("Download update"), ButtonBar.ButtonData.APPLY);
        ButtonType btnRemindMeLater = new ButtonType(Localization.lang("Remind me later"), ButtonBar.ButtonData.CANCEL_CLOSE);
        this.getDialogPane().getButtonTypes().addAll(btnIgnoreUpdate, btnDownloadUpdate, btnRemindMeLater);
        this.setResultConverter(button -> {
            if (button == btnIgnoreUpdate) {
                return false;
            } else if (button == btnDownloadUpdate) {
                NativeDesktop.openBrowserShowPopup(Version.JABREF_DOWNLOAD_URL, dialogService, externalApplicationsPreferences);
            }
            return true;
        });
        Button defaultButton = (Button) this.getDialogPane().lookupButton(btnDownloadUpdate);
        defaultButton.setDefaultButton(true);

        Hyperlink lblMoreInformation = new Hyperlink(Localization.lang("See what's new"));
        lblMoreInformation.setOnAction(event ->
                NativeDesktop.openBrowserShowPopup(latestVersion.getChangelogUrl(), dialogService, externalApplicationsPreferences)
        );

        VBox container = new VBox(
                new Label(Localization.lang("A new version of JabRef is available!")),
                new Label(Localization.lang("Latest version") + ": " + latestVersion.getFullVersion()),
                new Label(Localization.lang("Installed version") + ": " + currentVersion.getFullVersion()),
                lblMoreInformation
        );
        getDialogPane().setContent(container);
        getDialogPane().setPrefWidth(450);
    }
}
