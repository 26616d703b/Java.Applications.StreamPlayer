package com.ampersand.sp;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.ampersand.lcu.gui.color.ColorPalette;
import com.ampersand.lcu.gui.component.button.HighlightButton;
import com.ampersand.lcu.gui.component.field.TextValidationField;
import com.ampersand.lcu.validator.Validator;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

public class YoutubeStreamPlayer extends JFrame {

	/*
	 * Attributes:
	 */
	private static final long serialVersionUID = 1958354700649181328L;

	private FileListener m_file_listener;
	private HelpListener m_help_listener;

	// GUI

	private JMenuBar m_menu_bar;

	private JMenu m_file_menu;
	private JMenuItem m_exit;

	private JMenu m_help_menu;
	private JMenuItem m_about;

	// NORTH
	private TextValidationField m_url_field;

	// CENTER
	private EmbeddedMediaPlayer m_embedded_media_player;

	// SOUTH
	private HighlightButton m_play_button;

	/*
	 * Methods:
	 */

	// CONSTRUCTOR

	public YoutubeStreamPlayer() {

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

		// Window properties
		setSize(1024, 620);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setIconImage(new ImageIcon(getClass().getResource("res/icons/application.png")).getImage());
		setTitle("StreamPlayer - Youtube");
		setLocationRelativeTo(null);

		initialize();
	}

	// INITIALIZATIONS:

	public void initialize() {

		initMenu();

		// NORTH

		m_url_field = new TextValidationField(Validator.YOUTUBE_URL);

		add(m_url_field, BorderLayout.NORTH);

		// CENTER

		final MediaPlayerFactory media_player_factory = new MediaPlayerFactory();

		final Canvas video_surface = new Canvas();
		video_surface.setBackground(ColorPalette.BLACK);

		m_embedded_media_player = media_player_factory.newEmbeddedMediaPlayer();
		m_embedded_media_player.setVideoSurface(media_player_factory.newVideoSurface(video_surface));
		m_embedded_media_player.setPlaySubItems(true);

		add(video_surface, BorderLayout.CENTER);

		// SOUTH

		m_play_button = new HighlightButton("Jouer", ColorPalette.WHITE, ColorPalette.BLACK, ColorPalette.GRAY);
		m_play_button.addActionListener(event -> {

			if (Validator.YOUTUBE_URL.isValid(m_url_field.getText())) {

				m_embedded_media_player.playMedia(m_url_field.getText());
			}
		});

		add(m_play_button, BorderLayout.SOUTH);
	}

	public void initMenu() {

		// FILE

		m_exit = new JMenuItem("Quitter", new ImageIcon(getClass().getResource("res/icons/menu/shutdown.png")));
		m_exit.addActionListener(m_file_listener);
		m_exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
		m_exit.setMnemonic('q');

		m_file_menu = new JMenu("Fichier");
		m_file_menu.add(m_exit);
		m_file_menu.setMnemonic('f');

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
		m_menu_bar.add(m_help_menu);

		setJMenuBar(m_menu_bar);
	}

	// LISTENERS

	public class FileListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource().equals(m_exit)) {

				m_embedded_media_player.stop();
				m_embedded_media_player.release();

				System.exit(0);
			}
		}
	}

	public class HelpListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource().equals(m_about)) {

				JOptionPane.showMessageDialog(null, "StreamPlayer - Youtube est un projet réalisé pour le fun!",
						"À propos", JOptionPane.INFORMATION_MESSAGE,
						new ImageIcon(getClass().getResource("res/icons/application.png")));
			}
		}
	}

	// OTHER

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
