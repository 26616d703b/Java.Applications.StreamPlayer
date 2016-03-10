package com.ampersand.sp;

import javax.swing.SwingUtilities;

public class Main {

	public static void main(String[] args) {

		SwingUtilities.invokeLater(() -> {

			final LiveStreamPlayer window = new LiveStreamPlayer();
			// VODStreamPlayer window = new VODStreamPlayer();
			// YoutubeStreamPlayer window = new YoutubeStreamPlayer();

			window.setVisible(true);
		});
	}
}
