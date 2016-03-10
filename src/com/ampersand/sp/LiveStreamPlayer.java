package com.ampersand.sp;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.ampersand.lcu.gui.GUIFactory;
import com.ampersand.lcu.gui.color.ColorPalette;
import com.ampersand.lcu.gui.component.button.HighlightButton;
import com.ampersand.lcu.gui.component.field.TextValidationField;
import com.ampersand.lcu.validator.Validator;
import com.ampersand.sp.MetaStreamClient.ClientRequest;

public class LiveStreamPlayer extends JFrame {

	/*
	 * Attributes:
	 */
	private static final long serialVersionUID = -5351068348189950033L;

	private final LiveStreamPlayer m_instance = this;

	private FileListener m_file_listener;
	private SettingsListener m_settings_listener;
	private MediaListListener m_media_list_listener;
	private HelpListener m_help_listener;
	private ControlsbarListener m_controlsbar_listener;

	private final RtpStreamClient m_rtp_client;
	private MetaStreamClient m_meta_client;

	// GUI

	private JMenuBar m_menu_bar;

	private JMenu m_file_menu;
	private JMenuItem m_connect;
	private JMenuItem m_exit;

	private JMenu m_settings_menu;
	private JMenuItem m_network;

	private JMenu m_media_list_menu;
	private JMenuItem m_show;

	private JMenu m_help_menu;
	private JMenuItem m_about;

	private JPanel m_control_pane;
	private JSlider m_volume_slider;
	private HighlightButton m_mute_button;

	/*
	 * Methods:
	 */

	// CONSTRUCTOR

	public LiveStreamPlayer() {

		try {

			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (final ClassNotFoundException e) {

			e.printStackTrace();
		} catch (final InstantiationException e) {

			e.printStackTrace();
		} catch (final IllegalAccessException e) {

			e.printStackTrace();
		} catch (final UnsupportedLookAndFeelException e) {

			e.printStackTrace();
		}

		m_rtp_client = new RtpStreamClient("127.0.0.1", 5555);

		// Window properties
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {

				m_rtp_client.shutDown();
			}
		});

		setSize(1024, 620);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setIconImage(new ImageIcon(getClass().getResource("res/icons/application.png")).getImage());
		setTitle("StreamPlayer - Live");
		setLocationRelativeTo(null);
		setContentPane(m_rtp_client.getMediaPlayerComponent());

		initialize();
	}

	// INITIALIZATIONS:

	public void initialize() {

		initMenu();
		initControlbar();
	}

	public void initMenu() {

		// FILE

		m_file_listener = new FileListener();

		m_connect = new JMenuItem("Se connecter", new ImageIcon(getClass().getResource("res/icons/menu/online.png")));
		m_connect.addActionListener(m_file_listener);
		m_connect.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
		m_connect.setMnemonic('c');

		m_exit = new JMenuItem("Quitter", new ImageIcon(getClass().getResource("res/icons/menu/shutdown.png")));
		m_exit.addActionListener(m_file_listener);
		m_exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
		m_exit.setMnemonic('q');

		m_file_menu = new JMenu("Fichier");
		m_file_menu.add(m_connect);
		m_file_menu.addSeparator();
		m_file_menu.add(m_exit);
		m_file_menu.setMnemonic('f');

		// NETWORK

		m_settings_listener = new SettingsListener();

		m_network = new JMenuItem("Réseau", new ImageIcon(getClass().getResource("res/icons/menu/network.png")));
		m_network.addActionListener(m_settings_listener);
		m_network.setMnemonic('r');

		m_settings_menu = new JMenu("Paramètres");
		m_settings_menu.add(m_network);
		m_settings_menu.setMnemonic('p');

		// PLAYLIST

		m_media_list_listener = new MediaListListener();

		m_show = new JMenuItem("Afficher", new ImageIcon(getClass().getResource("res/icons/menu/list.png")));
		m_show.addActionListener(m_media_list_listener);
		m_show.setMnemonic('a');

		m_media_list_menu = new JMenu("Liste de lecture");
		m_media_list_menu.add(m_show);
		m_media_list_menu.setMnemonic('l');

		// HELP

		m_help_listener = new HelpListener();

		m_about = new JMenuItem("À propos", new ImageIcon(getClass().getResource("res/icons/menu/info.png")));
		m_about.addActionListener(m_help_listener);
		m_about.setAccelerator(KeyStroke.getKeyStroke("F1"));
		m_about.setMnemonic('p');

		m_help_menu = new JMenu("?");
		m_help_menu.add(m_about);
		m_help_menu.setMnemonic('?');

		// Menu

		m_menu_bar = new JMenuBar();
		m_menu_bar.add(m_file_menu);
		m_menu_bar.add(m_settings_menu);
		m_menu_bar.add(m_media_list_menu);
		m_menu_bar.add(m_help_menu);

		setJMenuBar(m_menu_bar);
	}

	public void initControlbar() {

		m_controlsbar_listener = new ControlsbarListener();

		// NORTH

		// CENTER

		m_volume_slider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 100);
		m_volume_slider.addChangeListener(m_controlsbar_listener);
		m_volume_slider.setBorder(new LineBorder(ColorPalette.BLACK, 2));
		m_volume_slider.setFocusable(false);

		m_mute_button = new HighlightButton("100",
				new ImageIcon(getClass().getResource("res/icons/controls/speaker.png")), ColorPalette.WHITE,
				ColorPalette.BLACK, ColorPalette.LIGHT_GRAY);

		m_mute_button.addActionListener(m_controlsbar_listener);
		m_mute_button.setPreferredSize(new Dimension(110, 30));
		m_mute_button.setToolTipText("Mute");

		final JPanel volume_pane = new JPanel(new BorderLayout());
		volume_pane.add(m_volume_slider, BorderLayout.CENTER);
		volume_pane.add(m_mute_button, BorderLayout.EAST);

		// --
		m_control_pane = new JPanel(new BorderLayout());
		m_control_pane.add(volume_pane, BorderLayout.CENTER);

		getContentPane().add(m_control_pane, BorderLayout.SOUTH);
	}

	// LISTENERS

	public class FileListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource().equals(m_connect)) {

				m_rtp_client.start();

				m_connect.setEnabled(false);
				m_network.setEnabled(false);
			} else if (event.getSource().equals(m_exit)) {

				m_rtp_client.shutDown();

				System.exit(0);
			}
		}
	}

	public class SettingsListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource().equals(m_network)) {

				final JDialog dialog = GUIFactory.createDialog(m_instance, "Paramètres", 100, 200);
				dialog.getContentPane().setLayout(new GridLayout(3, 1));

				final TextValidationField address_field = new TextValidationField(Validator.IP_ADDRESS);
				address_field.setText(m_rtp_client.getAddress());

				final TextValidationField port_field = new TextValidationField(Validator.PORT_NUMBER);
				port_field.setText(String.valueOf(m_rtp_client.getPort()));

				final HighlightButton validate_button = new HighlightButton(
						new ImageIcon(getClass().getResource("res/icons/menu/accept.png")), ColorPalette.WHITE,
						ColorPalette.LIGHT_GRAY);

				validate_button.addActionListener(event1 -> {

					if (Validator.IP_ADDRESS.isValid(address_field.getText())
							&& Validator.PORT_NUMBER.isValid(port_field.getText())) {

						m_rtp_client.setAddress(address_field.getText());
						m_rtp_client.setPort(Integer.valueOf(port_field.getText()));

						dialog.dispose();
					}
				});

				dialog.add(address_field);
				dialog.add(port_field);
				dialog.add(validate_button);

				dialog.setVisible(true);
			}
		}
	}

	public class MediaListListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource().equals(m_show)) {

				m_meta_client = new MetaStreamClient(m_rtp_client.getAddress(), 4444, ClientRequest.MEDIA_LIST);
				m_meta_client.start();

				try {

					Thread.sleep(1000);
				} catch (final InterruptedException e) {

					e.printStackTrace();
				}

				final JDialog dialog = GUIFactory.createDialog(m_instance, "La liste de lecture", 450, 600, false);
				dialog.setContentPane(new JScrollPane(m_meta_client.getMediaList()));
				dialog.setVisible(true);

				m_meta_client.ShutDown();
			}
		}
	}

	public class HelpListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource().equals(m_about)) {

				JOptionPane.showMessageDialog(m_instance, "StreamPlayer - Live est un projet réalisé pour le fun!",
						"À propos", JOptionPane.INFORMATION_MESSAGE,
						new ImageIcon(getClass().getResource("res/icons/application.png")));
			}
		}
	}

	public class ControlsbarListener implements ActionListener, ChangeListener {

		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource().equals(m_mute_button)) {

				if (m_mute_button.getToolTipText().equals("Mute")) {

					m_rtp_client.getMediaPlayerComponent().getMediaPlayer().mute(true);

					m_mute_button.setToolTipText("7Mute");
					m_mute_button.setIcon(new ImageIcon(getClass().getResource("res/icons/controls/mute.png")));

					m_volume_slider.setValue(0);
				} else {

					m_rtp_client.getMediaPlayerComponent().getMediaPlayer().mute(false);

					m_mute_button.setToolTipText("Mute");
					m_mute_button.setIcon(new ImageIcon(getClass().getResource("res/icons/controls/speaker.png")));

					m_volume_slider.setValue(100);
				}
			}
		}

		@Override
		public void stateChanged(ChangeEvent event) {

			m_rtp_client.getMediaPlayerComponent().getMediaPlayer().setVolume(m_volume_slider.getValue());

			if (m_mute_button.getToolTipText().equals("Mute") && m_volume_slider.getValue() == 0) {

				m_rtp_client.getMediaPlayerComponent().getMediaPlayer().mute(true);

				m_mute_button.setToolTipText("7Mute");
				m_mute_button.setIcon(new ImageIcon(getClass().getResource("res/icons/controls/mute.png")));
			} else if (m_mute_button.getToolTipText().equals("7Mute") && m_volume_slider.getValue() > 0) {

				m_rtp_client.getMediaPlayerComponent().getMediaPlayer().mute(false);

				m_mute_button.setToolTipText("Mute");
				m_mute_button.setIcon(new ImageIcon(getClass().getResource("res/icons/controls/speaker.png")));
			}

			m_mute_button.setText(String.valueOf(m_volume_slider.getValue()));
		}
	}
}
