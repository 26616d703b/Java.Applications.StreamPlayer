package com.ampersand.sp;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
import com.ampersand.lcu.gui.component.list.FilesList;
import com.ampersand.lcu.validator.Validator;
import com.ampersand.sp.MetaStreamClient.ClientRequest;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventListener;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.runtime.x.LibXUtil;

public class VODStreamPlayer extends JFrame {

	/*
	 * Attributes:
	 */
	private static final long serialVersionUID = -5351068348189950033L;

	private final VODStreamPlayer m_instance = this;

	private FileListener m_file_listener;
	private SettingsListener m_settings_listener;
	private MediaListListener m_media_list_listener;
	private HelpListener m_help_listener;
	private MediaPlayerListener m_media_player_listener;
	private ControlsbarListener m_controlsbar_listener;

	private String m_server_address = "127.0.0.1";
	private int m_server_port = 5555;
	private String m_stream_id;
	private int m_stream_index = 0;
	private MetaStreamClient m_meta_client;

	// GUI

	private JMenuBar m_menu_bar;

	private JMenu m_file_menu;
	private JMenuItem m_connect;
	private JMenuItem m_exit;

	private JMenu m_settings_menu;
	private JMenuItem m_network;

	private JMenu m_media_list_menu;
	private JMenuItem m_show_playlist;

	private JMenu m_help_menu;
	private JMenuItem m_about;

	private EmbeddedMediaPlayerComponent m_media_player_component;

	private JPanel m_control_pane;

	private JSlider m_progression_slider;
	private JLabel m_progression_label;

	private HighlightButton m_previous_button;
	private HighlightButton m_backward_button;
	private HighlightButton m_play_button;
	private HighlightButton m_forward_button;
	private HighlightButton m_next_button;

	private JSlider m_volume_slider;
	private HighlightButton m_mute_button;

	/*
	 * Methods:
	 */

	// CONSTRUCTOR

	public VODStreamPlayer() {

		// Initialise LibX pour réduire les opportunités de crash
		LibXUtil.initialise();

		// Ajout du chemin de la librairie vlc, puis chargement de cette
		// dernière
		NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), nativeLibrarySearchPath());
		Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);

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

		setSize(1024, 620);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setIconImage(new ImageIcon(getClass().getResource("res/icons/application.png")).getImage());
		setTitle("StreamPlayer - VOD");
		setLocationRelativeTo(null);

		initialize();
	}

	// INITIALIZATIONS:

	public void initialize() {

		initMenu();
		initMediaPlayer();
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

		m_show_playlist = new JMenuItem("Afficher", new ImageIcon(getClass().getResource("res/icons/menu/list.png")));
		m_show_playlist.addActionListener(m_media_list_listener);
		m_show_playlist.setMnemonic('a');

		m_media_list_menu = new JMenu("Liste de lecture");
		m_media_list_menu.add(m_show_playlist);
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

	public void initMediaPlayer() {

		m_media_player_listener = new MediaPlayerListener();

		m_media_player_component = new EmbeddedMediaPlayerComponent();
		m_media_player_component.getMediaPlayer().addMediaPlayerEventListener(m_media_player_listener);

		setContentPane(m_media_player_component);
	}

	public void initControlbar() {

		m_controlsbar_listener = new ControlsbarListener();

		// NORTH

		m_progression_slider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 0);
		m_progression_slider.addChangeListener(m_controlsbar_listener);
		m_progression_slider.setFocusable(false);

		m_progression_label = new JLabel("[00:00:00/00:00:00]");
		m_progression_label.setFont(new Font("Century Gothic", Font.PLAIN, 11));
		m_progression_label.setPreferredSize(new Dimension(110, 30));

		final JPanel progression_panel = new JPanel(new BorderLayout());
		progression_panel.add(m_progression_slider, BorderLayout.CENTER);
		progression_panel.add(m_progression_label, BorderLayout.EAST);

		// CENTER

		m_previous_button = new HighlightButton(
				new ImageIcon(getClass().getResource("res/icons/controls/previous.png")), ColorPalette.WHITE,
				ColorPalette.LIGHT_GRAY);

		m_previous_button.addActionListener(m_controlsbar_listener);
		m_previous_button.setToolTipText("Video Précédente");

		m_backward_button = new HighlightButton(
				new ImageIcon(getClass().getResource("res/icons/controls/backward.png")), ColorPalette.WHITE,
				ColorPalette.LIGHT_GRAY);

		m_backward_button.addMouseListener(m_controlsbar_listener);
		m_backward_button.setToolTipText("Reculer");

		m_play_button = new HighlightButton(new ImageIcon(getClass().getResource("res/icons/controls/pause.png")),
				ColorPalette.WHITE, ColorPalette.LIGHT_GRAY);

		m_play_button.addActionListener(m_controlsbar_listener);
		m_play_button.setToolTipText("Pause");

		m_forward_button = new HighlightButton(new ImageIcon(getClass().getResource("res/icons/controls/forward.png")),
				ColorPalette.WHITE, ColorPalette.LIGHT_GRAY);

		m_forward_button.addMouseListener(m_controlsbar_listener);
		m_forward_button.setToolTipText("Avancer");

		m_next_button = new HighlightButton(new ImageIcon(getClass().getResource("res/icons/controls/next.png")),
				ColorPalette.WHITE, ColorPalette.LIGHT_GRAY);

		m_next_button.addActionListener(m_controlsbar_listener);
		m_next_button.setToolTipText("Video Suivante");

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

		final JPanel center_pane = new JPanel(new GridLayout(1, 5));
		center_pane.add(m_previous_button);
		center_pane.add(m_backward_button);
		center_pane.add(m_play_button);
		center_pane.add(m_forward_button);
		center_pane.add(m_next_button);

		final JPanel volume_pane = new JPanel(new BorderLayout());
		volume_pane.add(m_volume_slider, BorderLayout.CENTER);
		volume_pane.add(m_mute_button, BorderLayout.EAST);

		// --
		m_control_pane = new JPanel(new BorderLayout());
		m_control_pane.add(progression_panel, BorderLayout.NORTH);
		m_control_pane.add(center_pane, BorderLayout.CENTER);
		m_control_pane.add(volume_pane, BorderLayout.EAST);

		getContentPane().add(m_control_pane, BorderLayout.SOUTH);
	}

	// LISTENERS

	public class FileListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource().equals(m_connect)) {

				m_meta_client = new MetaStreamClient(m_server_address, 4444, ClientRequest.MEDIA_LIST);
				m_meta_client.start();

				try {

					Thread.sleep(1000);
				} catch (final InterruptedException e) {

					e.printStackTrace();
				}

				final JDialog dialog = GUIFactory.createDialog(m_instance, "La liste de lecture", 450, 600, false);

				final FilesList media_list = m_meta_client.getMediaList();
				media_list.addMouseListener(new MouseAdapter() {

					@Override
					public void mouseClicked(MouseEvent event) {

						if (event.getClickCount() == 2) {

							m_stream_index = media_list.getSelectedIndex();
							m_stream_id = formatStreamId(media_list.getModel().getElementAt(m_stream_index).getName());

							m_media_player_component.getMediaPlayer()
									.playMedia(formatRtspStream(m_server_address, m_server_port, m_stream_id));

							dialog.dispose();
						}
					}
				});

				media_list.setSelectedIndex(m_stream_index);

				dialog.setContentPane(new JScrollPane(media_list));
				dialog.setVisible(true);

				m_meta_client.ShutDown();

				m_connect.setEnabled(false);
				m_network.setEnabled(false);
			} else if (event.getSource().equals(m_exit)) {

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
				address_field.setText(m_server_address);

				final TextValidationField port_field = new TextValidationField(Validator.PORT_NUMBER);
				port_field.setText(String.valueOf(m_server_port));

				final HighlightButton validate_button = new HighlightButton(
						new ImageIcon(getClass().getResource("res/icons/menu/accept.png")), ColorPalette.WHITE,
						ColorPalette.LIGHT_GRAY);

				validate_button.addActionListener(event1 -> {

					if (Validator.IP_ADDRESS.isValid(address_field.getText())
							&& Validator.PORT_NUMBER.isValid(port_field.getText())) {

						m_server_address = address_field.getText();
						m_server_port = Integer.valueOf(port_field.getText());

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

			if (event.getSource().equals(m_show_playlist)) {

				m_meta_client = new MetaStreamClient(m_server_address, 4444, ClientRequest.MEDIA_LIST);
				m_meta_client.start();

				try {

					Thread.sleep(1000);
				} catch (final InterruptedException e) {

					e.printStackTrace();
				}

				final JDialog dialog = GUIFactory.createDialog(m_instance, "La liste de lecture", 450, 600, false);

				final FilesList media_list = m_meta_client.getMediaList();
				media_list.addMouseListener(new MouseAdapter() {

					@Override
					public void mouseClicked(MouseEvent event) {

						if (event.getClickCount() == 2) {

							m_stream_index = media_list.getSelectedIndex();
							m_stream_id = formatStreamId(media_list.getModel().getElementAt(m_stream_index).getName());

							m_media_player_component.getMediaPlayer()
									.playMedia(formatRtspStream(m_server_address, m_server_port, m_stream_id));

							dialog.dispose();
						}
					}
				});

				media_list.setSelectedIndex(m_stream_index);

				dialog.setContentPane(new JScrollPane(media_list));
				dialog.setVisible(true);

				m_meta_client.ShutDown();
			}
		}
	}

	public class HelpListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource().equals(m_about)) {

				JOptionPane.showMessageDialog(m_instance, "StreamPlayer - VOD est un projet réalisé pour le fun!",
						"À propos", JOptionPane.INFORMATION_MESSAGE,
						new ImageIcon(getClass().getResource("res/icons/application.png")));
			}
		}
	}

	public class MediaPlayerListener implements MediaPlayerEventListener {

		@Override
		public void backward(MediaPlayer media_player) {
		}

		@Override
		public void buffering(MediaPlayer media_player, float arg1) {
		}

		@Override
		public void endOfSubItems(MediaPlayer media_player) {
		}

		@Override
		public void error(MediaPlayer media_player) {
		}

		@Override
		public void finished(MediaPlayer media_player) {
		}

		@Override
		public void forward(MediaPlayer media_player) {
		}

		@Override
		public void lengthChanged(MediaPlayer media_player, long arg1) {
		}

		@Override
		public void mediaChanged(MediaPlayer media_player, libvlc_media_t arg1, String arg2) {
		}

		@Override
		public void mediaDurationChanged(MediaPlayer media_player, long arg1) {
		}

		@Override
		public void mediaFreed(MediaPlayer media_player) {
		}

		@Override
		public void mediaMetaChanged(MediaPlayer media_player, int arg1) {
		}

		@Override
		public void mediaParsedChanged(MediaPlayer media_player, int arg1) {
		}

		@Override
		public void mediaStateChanged(MediaPlayer media_player, int arg1) {
		}

		@Override
		public void mediaSubItemAdded(MediaPlayer media_player, libvlc_media_t arg1) {
		}

		@Override
		public void newMedia(MediaPlayer media_player) {
		}

		@Override
		public void opening(MediaPlayer media_player) {
		}

		@Override
		public void pausableChanged(MediaPlayer media_player, int arg1) {
		}

		@Override
		public void paused(MediaPlayer media_player) {
		}

		@Override
		public void playing(MediaPlayer media_player) {
		}

		@Override
		public void positionChanged(MediaPlayer media_player, float arg1) {
		}

		@Override
		public void seekableChanged(MediaPlayer media_player, int arg1) {
		}

		@Override
		public void snapshotTaken(MediaPlayer media_player, String arg1) {
		}

		@Override
		public void stopped(MediaPlayer media_player) {
		}

		@Override
		public void subItemFinished(MediaPlayer media_player, int arg1) {
		}

		@Override
		public void subItemPlayed(MediaPlayer media_player, int arg1) {
		}

		@Override
		public void timeChanged(MediaPlayer media_player, long arg1) {

			final long time = m_media_player_component.getMediaPlayer().getTime();
			final long length = m_media_player_component.getMediaPlayer().getLength();

			// m_progression_slider.setValue((int)(((double)time/length)*100));
			m_progression_label.setText(formatProgression(time, length));
		}

		@Override
		public void titleChanged(MediaPlayer media_player, int arg1) {
		}

		@Override
		public void videoOutput(MediaPlayer media_player, int arg1) {
		}
	}

	public class ControlsbarListener implements ActionListener, ChangeListener, MouseListener {

		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource().equals(m_previous_button)) {

				m_stream_index--;

				if (m_stream_index < 0) {

					m_stream_index = m_meta_client.getMediaList().getModel().getSize() - 1;
				}

				m_stream_id = formatStreamId(
						m_meta_client.getMediaList().getModel().getElementAt(m_stream_index).getName());

				m_media_player_component.getMediaPlayer()
						.playMedia(formatRtspStream(m_server_address, m_server_port, m_stream_id));
			} else if (event.getSource().equals(m_play_button)) {

				m_media_player_component.getMediaPlayer().pause();

				if (m_play_button.getToolTipText().equals("Pause")) {

					m_play_button.setToolTipText("Reprendre");
					m_play_button.setIcon(new ImageIcon(getClass().getResource("res/icons/controls/play.png")));
				} else {

					m_play_button.setToolTipText("Pause");
					m_play_button.setIcon(new ImageIcon(getClass().getResource("res/icons/controls/pause.png")));
				}
			} else if (event.getSource().equals(m_next_button)) {

				m_stream_index++;

				if (m_stream_index == m_meta_client.getMediaList().getModel().getSize()) {

					m_stream_index = 0;
				}

				m_stream_id = formatStreamId(
						m_meta_client.getMediaList().getModel().getElementAt(m_stream_index).getName());

				m_media_player_component.getMediaPlayer()
						.playMedia(formatRtspStream(m_server_address, m_server_port, m_stream_id));
			} else if (event.getSource().equals(m_mute_button)) {

				if (m_mute_button.getToolTipText().equals("Mute")) {

					m_media_player_component.getMediaPlayer().mute(true);

					m_mute_button.setToolTipText("7Mute");
					m_mute_button.setIcon(new ImageIcon(getClass().getResource("res/icons/controls/mute.png")));

					m_volume_slider.setValue(0);
				} else {

					m_media_player_component.getMediaPlayer().mute(false);

					m_mute_button.setToolTipText("Mute");
					m_mute_button.setIcon(new ImageIcon(getClass().getResource("res/icons/controls/speaker.png")));

					m_volume_slider.setValue(100);
				}
			}
		}

		@Override
		public void mouseClicked(MouseEvent event) {
		}

		@Override
		public void mouseEntered(MouseEvent event) {
		}

		@Override
		public void mouseExited(MouseEvent event) {
		}

		@Override
		public void mousePressed(MouseEvent event) {

			if (event.getSource().equals(m_backward_button)) {

				m_media_player_component.getMediaPlayer()
						.setTime(m_media_player_component.getMediaPlayer().getTime() - 10000);
			} else if (event.getSource().equals(m_forward_button)) {

				m_media_player_component.getMediaPlayer()
						.setTime(m_media_player_component.getMediaPlayer().getTime() + 10000);
			}
		}

		@Override
		public void mouseReleased(MouseEvent event) {
		}

		@Override
		public void stateChanged(ChangeEvent event) {

			if (event.getSource().equals(m_progression_slider)) {

				final long progress = m_progression_slider.getValue()
						* m_media_player_component.getMediaPlayer().getLength() / 100;

				m_media_player_component.getMediaPlayer().setTime(progress);
			} else if (event.getSource().equals(m_volume_slider)) {

				m_media_player_component.getMediaPlayer().setVolume(m_volume_slider.getValue());

				if (m_mute_button.getToolTipText().equals("Mute") && m_volume_slider.getValue() == 0) {

					m_media_player_component.getMediaPlayer().mute(true);

					m_mute_button.setToolTipText("7Mute");
					m_mute_button.setIcon(new ImageIcon(getClass().getResource("res/icons/controls/mute.png")));
				} else if (m_mute_button.getToolTipText().equals("7Mute") && m_volume_slider.getValue() > 0) {

					m_media_player_component.getMediaPlayer().mute(false);

					m_mute_button.setToolTipText("Mute");
					m_mute_button.setIcon(new ImageIcon(getClass().getResource("res/icons/controls/speaker.png")));
				}

				m_mute_button.setText(String.valueOf(m_volume_slider.getValue()));
			}
		}
	}

	// RE-IMPLEMENTED METHODS

	// IMPLEMENTED METHODS

	private static String formatProgression(long time, long length) {

		final String ftime = String.format("%02d:%02d:%02d",

				TimeUnit.MILLISECONDS.toHours(time),
				TimeUnit.MILLISECONDS.toMinutes(time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(time)),
				TimeUnit.MILLISECONDS.toSeconds(time)
						- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time)));

		final String flength = String.format("%02d:%02d:%02d",

				TimeUnit.MILLISECONDS.toHours(length),
				TimeUnit.MILLISECONDS.toMinutes(length)
						- TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(length)),
				TimeUnit.MILLISECONDS.toSeconds(length)
						- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(length)));

		return "[" + ftime + "/" + flength + "]";
	}

	private static String formatRtspStream(String server_address, int server_port, String stream_id) {

		final StringBuilder string_builder = new StringBuilder(30);
		string_builder.append("rtsp://@");
		string_builder.append(server_address);
		string_builder.append(":");
		string_builder.append(server_port);
		string_builder.append("/");
		string_builder.append(stream_id);

		return string_builder.toString();
	}

	private static String formatStreamId(String fname) {

		return fname.substring(0, fname.lastIndexOf(".")).replace(" ", "_").replace("(", "[").replace(")", "]")
				.replace(".", "_");
	}

	private static String nativeLibrarySearchPath() {

		// Detecter l'OS de l'utilisateur pour améliorer la portabilité
		String native_library_search_path = null;
		final String user_os = System.getProperty("os.name");
		final String user_os_arch = System.getProperty("os.arch");

		if (user_os.startsWith("Windows")) {

			if (user_os_arch.contains("32")) {

				native_library_search_path = "natives/x86/win";
			} else {

				native_library_search_path = "natives/x64/win";
			}
		} else if (user_os.startsWith("Linux")) {

			if (user_os_arch.contains("32")) {

				native_library_search_path = "natives/x86/linux";
			} else {

				native_library_search_path = "natives/x64/linux";
			}
		} else if (user_os.startsWith("Mac")) {

			if (user_os_arch.contains("32")) {

				native_library_search_path = "natives/x86/mac";
			} else {

				native_library_search_path = "natives/x64/mac";
			}
		}

		return native_library_search_path;
	}
}
