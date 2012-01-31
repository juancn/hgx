package codng.hgx.ui;

import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class SearchableCombo<T> extends JComboBox<T> {
	public SearchableCombo() {
		super();
		init();
	}

	private void init () {
		setEditable(true);
		setEditor(new SearchEditor (this));
		setUI(new BasicComboBoxUI(){
			@Override
			protected JButton createArrowButton() {
				final ImageIcon icon = new ImageIcon(getClass().getResource("search.png"));
				final JButton jButton = new JButton(icon);
				jButton.setDisabledIcon(icon);
				jButton.setOpaque(true);
				jButton.setBackground(Color.WHITE);
				jButton.setBorderPainted(false);
				return jButton;
			}
		});
	}
 
	private static class SearchEditor extends BasicComboBoxEditor {
		public SearchEditor (final SearchableCombo cb) {
			final KeyAdapter listener = new KeyAdapter () {
				public void keyReleased (KeyEvent ev) {
					if ((ev.getKeyChar () >= 'a' && ev.getKeyChar () <= 'z') ||
							(ev.getKeyChar () >= '0' && ev.getKeyChar () <= '9') ||
							(ev.getKeyChar () >= 'A' && ev.getKeyChar () <= 'Z') ||
							(ev.getKeyChar () == KeyEvent.VK_SPACE))
					{
						final String typedText = editor.getText();
						cb.showPopup();
						int index = cb.findMatchingElement(typedText);
						if (index == -1) return;
						final String foundText = String.valueOf(cb.getModel().getElementAt(index));
						if (!foundText.equals(typedText) && foundText.startsWith(foundText)) {
							editor.setText(foundText);
							editor.setSelectionStart(typedText.length());
							editor.setSelectionEnd(foundText.length());
						}
						cb.setSelectedIndex(index);
					}
				}
			};
			editor.addKeyListener (listener);
			final ActionListener actionListener = new ActionListener () {
				public void actionPerformed (ActionEvent e) {
					if (cb.getSelectedItem () != null && !editor.getText().equals(String.valueOf(cb.getSelectedItem()))) {
						editor.setText (cb.getSelectedItem ().toString ());
					}
				}
			};
			cb.addActionListener(actionListener);
		}
	}

	protected int findMatchingElement(String text) {
		int index = -1;
		if (!text.isEmpty()) {
			final ComboBoxModel model = getModel();
			for (int i = 0; i < model.getSize(); i++) {
				final Object elementAt = model.getElementAt(i);
				if(String.valueOf(elementAt).toLowerCase().startsWith(text.toLowerCase())) {
					index = i;
					break;
				}
			}
		}
		return index;
	}
} 