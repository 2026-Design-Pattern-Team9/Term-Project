package com.holub.life;

import java.awt.*;
import javax.swing.*;
import com.holub.ui.MenuSite;
import com.holub.life.control.GameController;
import com.holub.life.control.GoMenu;
import com.holub.life.control.KeyBindings;

/*******************************************************************
 * An implemenation of Conway's Game of Life.
 *
 * @include /etc/license.txt
 */

public final class Life extends JFrame
{	
	private static JComponent universe;

	public static void main( String[] arguments )
	{	new Life();
	}

	private static final String TITLE =
			"The Game of Life. (c)2003 Allen I. Holub <http://www.holub.com>";

	private Life()
	{	super( TITLE );

		// Must establish the MenuSite very early in case
		// a subcomponent puts menus on it.
		MenuSite.establish( this );		//{=life.java.establish}

		setDefaultCloseOperation	( EXIT_ON_CLOSE 		);
		getContentPane().setLayout	( new BorderLayout()	);
		getContentPane().add( Universe.instance(), BorderLayout.CENTER); //{=life.java.install}
		RuleMenu.install( Universe.instance().model() );

		// 실행 상태 관리: 화면 입력, Go 메뉴, 단축키가 같은 컨트롤러를 쓴다.
		GameController controller =
				new GameController( Universe.instance().model(), Clock.instance() );
		Universe.instance().setController( controller );
		new GoMenu( controller ).install();
		KeyBindings.install( getRootPane(), controller );

		// 현재 상태를 제목 표시줄에 보여준다. (상태 패널이 생기면 그쪽에서 구독)
		showState( controller );
		controller.addStateListener
		(	state -> SwingUtilities.invokeLater( () -> showState( controller ) )
		);

		pack();
		setVisible( true );
	}

	private void showState( GameController controller )
	{	setTitle( TITLE + "  [" + controller.state().displayName() + "]" );
	}
}
