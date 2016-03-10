package com.ampersand.sp;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

public class RtspStreamClient extends Thread {

	/*
	 * Attributes
	 */

	private boolean m_running;

	private String m_server_address;
	private int m_server_port;
	private String m_stream_id;

	private final EmbeddedMediaPlayerComponent m_media_player_component;

	/*
	 * Methods
	 */

	// CONSTRUCTOR

	public RtspStreamClient(String server_address, int server_port, String stream_id) {

		// Ajout du chemin de la librairie vlc, puis chargement de cette
		// dernière
		NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), nativeLibrarySearchPath());
		Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);

		m_running = false;
		m_server_address = server_address;
		m_server_port = server_port;
		m_stream_id = stream_id;
		m_media_player_component = new EmbeddedMediaPlayerComponent();
		m_media_player_component.getMediaPlayer().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {

			@Override
			public void error(MediaPlayer mediaPlayer) {

			}
		});
	}

	// ACCESSORS and MUTATORS

	public boolean isRunning() {

		return m_running;
	}

	public String getAddress() {

		return m_server_address;
	}

	public void setAddress(String address) {

		m_server_address = address;
	}

	public int getPort() {

		return m_server_port;
	}

	public void setPort(int port) {

		m_server_port = port;
	}

	public String getStreamId() {

		return m_stream_id;
	}

	public void setStreamId(String stream_id) {

		m_stream_id = stream_id;
	}

	public EmbeddedMediaPlayerComponent getMediaPlayerComponent() {

		return m_media_player_component;
	}

	// RE-IMPLEMENTED METHODS

	@Override
	public void run() {

		if (!m_running) {

			m_running = m_media_player_component.getMediaPlayer()
					.playMedia(formatRtspStream(m_server_address, m_server_port, m_stream_id));

			// Pour empêcher la déconnexion

			try {

				join();
			} catch (final InterruptedException e) {

				Thread.currentThread().interrupt();
			}
		}
	}

	public void shutDown() {

		if (m_running) {

			m_media_player_component.release(true);

			interrupt();
		}

		m_running = false;
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
