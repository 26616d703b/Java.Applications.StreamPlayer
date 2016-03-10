package com.ampersand.sp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import com.ampersand.lcu.gui.component.list.FilesList;

public class MetaStreamClient extends Thread {

	/*
	 * Attributes
	 */

	private boolean m_running = false;

	private String m_host;
	private final int m_port;
	private final int m_request_id;
	private Socket m_clnt_socket;

	private ObjectInputStream m_obj_in_stream;
	private DataOutputStream m_data_out_stream;

	private FilesList m_media_list;

	
	/*
	 * Methods
	 */

	// CONSTRUCTOR

	public MetaStreamClient(String host, int port, int request_id) {

		m_host = host;
		m_port = port;
		m_request_id = request_id;
	}

	// ACCESSORS and MUTATORS

	public synchronized boolean isRunning() {

		return m_running;
	}

	public FilesList getMediaList() {

		return m_media_list;
	}

	public void setHostAddress(String address) {

		m_host = address;
	}

	// RE-IMPLEMENTED METHODS

	@Override
	public void run() {

		if (!m_running) {

			try {

				m_clnt_socket = new Socket(m_host, m_port);

				System.out.println("Client connecté!");

				m_running = true;

				m_data_out_stream = new DataOutputStream(m_clnt_socket.getOutputStream());

				try {

					System.out.println("Le client tente d'envoyer une requête!");
					m_data_out_stream.writeInt(m_request_id);
					System.out.println("Le client à envoyé la requête!");

					m_obj_in_stream = new ObjectInputStream(m_clnt_socket.getInputStream());
					System.out.println("Client en attente de réception...");
					m_media_list = (FilesList) m_obj_in_stream.readObject();
					System.out.println("Objet reçu!");
				} catch (final ClassNotFoundException e) {

					e.printStackTrace();
				}
			} catch (final UnknownHostException e) {

				e.printStackTrace();
			} catch (final IOException e) {

				e.printStackTrace();
			}
		}
	}

	// IMPLEMENTED METHODS

	public synchronized void ShutDown() {

		if (m_running) {

			try {

				m_obj_in_stream.close();
				m_clnt_socket.close();

				System.out.println("Arrêt du client...");
			} catch (final IOException e) {

				e.printStackTrace();
			}
		}

		m_running = false;
	}

	// IMPLEMENTED CLASSES

	public abstract class ClientRequest {

		public static final int MEDIA_LIST = 0;
	}
}
