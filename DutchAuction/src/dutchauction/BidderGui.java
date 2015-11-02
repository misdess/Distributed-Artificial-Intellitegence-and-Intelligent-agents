
package dutchauction;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


class BidderGui extends JFrame {	
	private CuratorAgent myAgent;
	
	private JTextField artifactField, priceField;
	
	BidderGui(CuratorAgent a) {
		super(a.getLocalName());
		
		myAgent = a;
		
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(2, 2));
		p.add(new JLabel("Artifact title:"));
		artifactField = new JTextField(15);
		p.add(artifactField);
		p.add(new JLabel("Price:"));
		priceField = new JTextField(15);
		p.add(priceField);
		getContentPane().add(p, BorderLayout.CENTER);
		
		JButton addButton = new JButton("Add");
		addButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					String aritifact = artifactField.getText().trim();
					String price = priceField.getText().trim();
					myAgent.updateCatalogue(aritifact, Integer.parseInt(price));
					artifactField.setText("");
					priceField.setText("");
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(BidderGui.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
				}
			}
		} );
		p = new JPanel();
		p.add(addButton);
		getContentPane().add(p, BorderLayout.SOUTH);
		
		// Make the agent terminate when the user closes 
		// the GUI using the button on the upper right corner	
		addWindowListener(new	WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.doDelete();
			}
		} );
		
		setResizable(false);
	}
	
	public void showGui() {
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)screenSize.getWidth() / 2;
		int centerY = (int)screenSize.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
		super.setVisible(true);
	}	
}
