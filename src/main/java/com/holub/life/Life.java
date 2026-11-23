package com.holub.life;

import java.awt.*;
import javax.swing.*;
import com.holub.ui.MenuSite;
import com.holub.life.control.GameController;
import com.holub.life.control.GoMenu;
import com.holub.life.control.KeyBindings;
import com.holub.life.history.GenerationHistory;

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

	/** 현재 세대를 포함해 보관할 세대 수 (C·D·F 연결 규격). */
	private static final int HISTORY_SIZE = 20;

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

		// 보드와 세대 이력은 여기서 한 번만 만들고 필요한 객체에 전달한다.
		BoardModel model = Universe.instance().model();
		GenerationHistory history =
				new GenerationHistory( HISTORY_SIZE, model.snapshot() );
		// 이력은 다른 세대 완료 리스너(F의 통계 등)보다 먼저 등록해야
		// 그 리스너가 history.period()를 읽을 때 방금 끝난 세대가 반영되어 있다.
		model.addStepListener( history );

		// 실행 상태 관리: 화면 입력, Go 메뉴, 단축키가 같은 컨트롤러를 쓴다.
		GameController controller =
				new GameController( model, Clock.instance(), history );
		Universe.instance().setController( controller );
		// 되돌리기·비우기처럼 세대 진행이 아닌 이유로 판이 바뀌면 다시 그린다.
		controller.addBoardListener( () -> Universe.instance().repaint() );
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
