/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2021 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.atlauncher.App;
import com.atlauncher.constants.Constants;
import com.atlauncher.constants.UIConstants;
import com.atlauncher.data.Instance;
import com.atlauncher.data.curseforge.CurseForgeProject;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;
import com.atlauncher.data.modrinth.ModrinthMod;
import com.atlauncher.data.modrinth.ModrinthSearchHit;
import com.atlauncher.data.modrinth.ModrinthSearchResult;
import com.atlauncher.gui.card.CurseForgeProjectCard;
import com.atlauncher.gui.card.ModrinthSearchHitCard;
import com.atlauncher.gui.panels.LoadingPanel;
import com.atlauncher.gui.panels.NoCurseModsPanel;
import com.atlauncher.network.Analytics;
import com.atlauncher.utils.ComboItem;
import com.atlauncher.utils.CurseForgeApi;
import com.atlauncher.utils.ModrinthApi;

import org.mini2Dx.gettext.GetText;

@SuppressWarnings("serial")
public final class AddModsDialog extends JDialog {
    private Instance instance;

    private boolean updating = false;

    private final JPanel contentPanel = new JPanel(new GridLayout(0, 2));
    private final JPanel topPanel = new JPanel(new BorderLayout());
    private final JTextField searchField = new JTextField(16);
    private final JButton searchButton = new JButton(GetText.tr("Search"));
    private final JComboBox<ComboItem<String>> hostComboBox = new JComboBox<ComboItem<String>>();
    private final JComboBox<ComboItem<String>> sectionComboBox = new JComboBox<ComboItem<String>>();
    private final JComboBox<ComboItem<String>> sortComboBox = new JComboBox<ComboItem<String>>();

    // #. Fabric API is the name of a mod, so should be left untranslated
    private final JButton installFabricApiButton = new JButton(GetText.tr("Install Fabric API"));

    // #. Fabric/Fabric API is the name of a mod, so should be left untranslated
    private final JLabel fabricApiWarningLabel = new JLabel(
            "<html><p align=\"center\" style=\"color: yellow\">Before installing Fabric mods, you should install Fabric API first!</p></html>");

    private JScrollPane jscrollPane;
    private JButton nextButton;
    private JButton prevButton;
    private final JPanel mainPanel = new JPanel(new BorderLayout());
    private int page = 0;

    public AddModsDialog(Instance instance) {
        this(App.launcher.getParent(), instance);
    }

    public AddModsDialog(Window parent, Instance instance) {
        // #. {0} is the name of the mod we're installing
        super(parent, GetText.tr("Adding Mods For {0}", instance.launcher.name), ModalityType.DOCUMENT_MODAL);
        this.instance = instance;

        this.setPreferredSize(new Dimension(680, 500));
        this.setResizable(false);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        contentPanel.setBorder(
                BorderFactory.createEmptyBorder(0, UIConstants.SPACING_LARGE * 10, 0, UIConstants.SPACING_LARGE * 10));

        hostComboBox.addItem(new ComboItem<>("CurseForge", "CurseForge"));
        hostComboBox.addItem(new ComboItem<>("Modrinth", "Modrinth"));
        hostComboBox.setSelectedIndex(App.settings.defaultModPlatform.equals("CurseForge") ? 0 : 1);

        if (instance.launcher.loaderVersion != null) {
            sectionComboBox.addItem(new ComboItem<>("Mods", GetText.tr("Mods")));
        }
        sectionComboBox.addItem(new ComboItem<>("Resource Packs", GetText.tr("Resource Packs")));
        sectionComboBox.addItem(new ComboItem<>("Worlds", GetText.tr("Worlds")));
        sectionComboBox.setVisible(App.settings.defaultModPlatform.equals("CurseForge"));

        if (App.settings.defaultModPlatform.equals("CurseForge")) {
            sortComboBox.addItem(new ComboItem<>("Popularity", GetText.tr("Popularity")));
            sortComboBox.addItem(new ComboItem<>("Last Updated", GetText.tr("Last Updated")));
            sortComboBox.addItem(new ComboItem<>("Total Downloads", GetText.tr("Total Downloads")));
        } else {
            sortComboBox.addItem(new ComboItem<>("relevance", GetText.tr("Relevance")));
            sortComboBox.addItem(new ComboItem<>("newest", GetText.tr("Newest")));
            sortComboBox.addItem(new ComboItem<>("updated", GetText.tr("Last Updated")));
            sortComboBox.addItem(new ComboItem<>("downloads", GetText.tr("Total Downloads")));
        }

        setupComponents();

        this.loadDefaultMods();

        this.pack();
        this.setLocationRelativeTo(App.launcher.getParent());
        this.setVisible(true);
    }

    private void setupComponents() {
        Analytics.sendScreenView("Add Mods Dialog");

        this.topPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JPanel searchButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        searchButtonsPanel.add(this.hostComboBox);
        searchButtonsPanel.add(this.searchField);
        searchButtonsPanel.add(this.searchButton);
        searchButtonsPanel.add(this.sectionComboBox);
        searchButtonsPanel.add(this.sortComboBox);

        this.installFabricApiButton.addActionListener(e -> {
            CurseForgeProject mod = CurseForgeApi.getProjectById(Constants.CURSEFORGE_FABRIC_MOD_ID);

            Analytics.sendEvent("AddFabricApi", "CurseForgeMod");
            new CurseForgeProjectFileSelectorDialog(mod, instance);

            if (instance.launcher.mods.stream().anyMatch(
                    m -> m.isFromCurseForge() && m.getCurseForgeModId() == Constants.CURSEFORGE_FABRIC_MOD_ID)) {
                fabricApiWarningLabel.setVisible(false);
                installFabricApiButton.setVisible(false);
            }
        });

        LoaderVersion loaderVersion = this.instance.launcher.loaderVersion;

        if (loaderVersion != null && loaderVersion.isFabric() && instance.launcher.mods.stream().noneMatch(
                mod -> mod.isFromCurseForge() && mod.getCurseForgeModId() == Constants.CURSEFORGE_FABRIC_MOD_ID)) {

            this.topPanel.add(fabricApiWarningLabel, BorderLayout.CENTER);
            this.topPanel.add(installFabricApiButton, BorderLayout.EAST);
        }

        this.topPanel.add(searchButtonsPanel, BorderLayout.NORTH);

        this.jscrollPane = new JScrollPane(this.contentPanel) {
            {
                this.getVerticalScrollBar().setUnitIncrement(16);
            }
        };

        this.jscrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        mainPanel.add(this.topPanel, BorderLayout.NORTH);
        mainPanel.add(this.jscrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout());

        prevButton = new JButton("<<");
        prevButton.setEnabled(false);
        prevButton.addActionListener(e -> goToPreviousPage());

        nextButton = new JButton(">>");
        nextButton.setEnabled(false);
        nextButton.addActionListener(e -> goToNextPage());

        bottomPanel.add(prevButton);
        bottomPanel.add(nextButton);

        this.add(mainPanel, BorderLayout.CENTER);
        this.add(bottomPanel, BorderLayout.SOUTH);

        this.hostComboBox.addActionListener(e -> {
            updating = true;
            boolean isCurseForge = ((ComboItem<String>) hostComboBox.getSelectedItem()).getValue()
                    .equalsIgnoreCase("CurseForge");

            sortComboBox.removeAllItems();
            if (isCurseForge) {
                sortComboBox.addItem(new ComboItem<>("Popularity", GetText.tr("Popularity")));
                sortComboBox.addItem(new ComboItem<>("Last Updated", GetText.tr("Last Updated")));
                sortComboBox.addItem(new ComboItem<>("Total Downloads", GetText.tr("Total Downloads")));
            } else {
                sortComboBox.addItem(new ComboItem<>("relevance", GetText.tr("Relevance")));
                sortComboBox.addItem(new ComboItem<>("newest", GetText.tr("Newest")));
                sortComboBox.addItem(new ComboItem<>("updated", GetText.tr("Last Updated")));
                sortComboBox.addItem(new ComboItem<>("downloads", GetText.tr("Total Downloads")));
            }

            sectionComboBox.setVisible(isCurseForge);

            if (searchField.getText().isEmpty()) {
                loadDefaultMods();
            } else {
                searchForMods();
            }
            updating = false;
        });

        this.sectionComboBox.addActionListener(e -> {
            if (!updating) {
                if (searchField.getText().isEmpty()) {
                    loadDefaultMods();
                } else {
                    searchForMods();
                }
            }
        });

        this.sortComboBox.addActionListener(e -> {
            if (!updating) {
                if (searchField.getText().isEmpty()) {
                    loadDefaultMods();
                } else {
                    searchForMods();
                }
            }
        });

        this.searchField.addActionListener(e -> searchForMods());

        this.searchButton.addActionListener(e -> searchForMods());
    }

    private void setLoading(boolean loading) {
        if (loading) {
            contentPanel.removeAll();
            contentPanel.setLayout(new BorderLayout());
            contentPanel.add(new LoadingPanel(), BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }

    private void goToPreviousPage() {
        if (page > 0) {
            page -= 1;
        }

        Analytics.sendEvent(page, "Previous", "Navigation", "CurseForgeMod");

        getMods();
    }

    private void goToNextPage() {
        if (contentPanel.getComponentCount() != 0) {
            page += 1;
        }

        Analytics.sendEvent(page, "Next", "Navigation", "CurseForgeMod");

        getMods();
    }

    @SuppressWarnings("unchecked")
    private void getMods() {
        setLoading(true);
        prevButton.setEnabled(false);
        nextButton.setEnabled(false);

        String query = searchField.getText();
        boolean isCurseForge = ((ComboItem<String>) hostComboBox.getSelectedItem()).getValue()
                .equalsIgnoreCase("CurseForge");

        new Thread(() -> {
            if (isCurseForge) {
                if (((ComboItem<String>) sectionComboBox.getSelectedItem()).getValue().equals("Resource Packs")) {
                    setCurseForgeMods(CurseForgeApi.searchResourcePacks(query, page,
                            ((ComboItem<String>) sortComboBox.getSelectedItem()).getValue()));
                } else if (((ComboItem<String>) sectionComboBox.getSelectedItem()).getValue().equals("Worlds")) {
                    setCurseForgeMods(
                            CurseForgeApi.searchWorlds(App.settings.disableAddModRestrictions ? null : this.instance.id,
                                    query, page, ((ComboItem<String>) sortComboBox.getSelectedItem()).getValue()));
                } else {
                    if (this.instance.launcher.loaderVersion.isFabric()) {
                        setCurseForgeMods(CurseForgeApi.searchModsForFabric(
                                App.settings.disableAddModRestrictions ? null : this.instance.id, query, page,
                                ((ComboItem<String>) sortComboBox.getSelectedItem()).getValue()));
                    } else {
                        setCurseForgeMods(CurseForgeApi.searchMods(
                                App.settings.disableAddModRestrictions ? null : this.instance.id, query, page,
                                ((ComboItem<String>) sortComboBox.getSelectedItem()).getValue()));
                    }
                }
            } else {
                if (this.instance.launcher.loaderVersion.isFabric()) {
                    setModrinthMods(ModrinthApi.searchModsForFabric(
                            App.settings.disableAddModRestrictions ? null : this.instance.id, query, page,
                            ((ComboItem<String>) sortComboBox.getSelectedItem()).getValue()));
                } else {
                    setModrinthMods(ModrinthApi.searchModsForForge(
                            App.settings.disableAddModRestrictions ? null : this.instance.id, query, page,
                            ((ComboItem<String>) sortComboBox.getSelectedItem()).getValue()));
                }
            }

            setLoading(false);
        }).start();
    }

    private void loadDefaultMods() {
        getMods();
    }

    private void searchForMods() {
        String query = searchField.getText();

        Analytics.sendEvent(query, "Search", "CurseForgeMod");

        getMods();
    }

    private void setCurseForgeMods(List<CurseForgeProject> mods) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets.set(2, 2, 2, 2);

        contentPanel.removeAll();

        prevButton.setEnabled(page > 0);
        nextButton.setEnabled(mods.size() == Constants.CURSEFORGE_PAGINATION_SIZE);

        if (mods.size() == 0) {
            contentPanel.setLayout(new BorderLayout());
            contentPanel.add(new NoCurseModsPanel(!this.searchField.getText().isEmpty()), BorderLayout.CENTER);
        } else {
            contentPanel.setLayout(new GridLayout(0, 2));

            mods.forEach(mod -> {
                CurseForgeProject castMod = (CurseForgeProject) mod;

                contentPanel.add(new CurseForgeProjectCard(castMod, e -> {
                    Analytics.sendEvent(castMod.name, "Add", "CurseForgeMod");
                    new CurseForgeProjectFileSelectorDialog(this, castMod, instance);
                }), gbc);

                gbc.gridy++;
            });
        }

        SwingUtilities.invokeLater(() -> jscrollPane.getVerticalScrollBar().setValue(0));

        revalidate();
        repaint();
    }

    private void setModrinthMods(ModrinthSearchResult searchResult) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets.set(2, 2, 2, 2);

        contentPanel.removeAll();

        prevButton.setEnabled(page > 0);
        nextButton.setEnabled((searchResult.offset + searchResult.limit) < searchResult.totalHits);

        if (searchResult.hits.size() == 0) {
            contentPanel.setLayout(new BorderLayout());
            contentPanel.add(new NoCurseModsPanel(!this.searchField.getText().isEmpty()), BorderLayout.CENTER);
        } else {
            contentPanel.setLayout(new GridLayout(0, 2));

            searchResult.hits.forEach(mod -> {
                ModrinthSearchHit castMod = (ModrinthSearchHit) mod;

                contentPanel.add(new ModrinthSearchHitCard(castMod, e -> {
                    ModrinthMod modrinthMod = ModrinthApi.getMod(castMod.modId);
                    Analytics.sendEvent(castMod.title, "Add", "ModrinthMod");
                    new ModrinthVersionSelectorDialog(this, modrinthMod, instance);
                }), gbc);

                gbc.gridy++;
            });
        }

        SwingUtilities.invokeLater(() -> jscrollPane.getVerticalScrollBar().setValue(0));

        revalidate();
        repaint();
    }
}
