package com.holub.life;

import java.io.*;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import com.holub.life.format.PatternFileActions;
import com.holub.ui.MenuSite;
import com.holub.life.control.GameController;

/**
 * Swing 입력을 모델에 전달하고 기존 보드를 그리는 패널.
 *
 * @include /etc/license.txt
 */
public class Universe extends JPanel
{
	private final Neighborhood boardToDraw;
	private final BoardModel model;
	private final PatternFileActions patternActions;
	/** 실행 상태(편집·실행·일시정지)를 관리한다. Life에서 연결한다. */
	private GameController controller;
	private static final Universe theInstance = new Universe();

	/** The size of the smallest "atomic" cell---a Resident object.
	 *  This size is extrinsic to a Resident (It's passed into the
	 *  Resident's "draw yourself" method.
	 */
	private static final int  DEFAULT_CELL_SIZE = 8;

	private Universe()
	{
		// 같은 보드를 모델이 갱신하고 패널은 기존 그리기 방식으로 표시한다.
		model = new BoardModel();
		boardToDraw = model.board();
		// 불러오기·저장·패턴 배치 전에는 일시정지하고,
		// 불러오기·패턴 배치 뒤에는 새 판이므로 편집 상태로 돌아간다.
		// controller는 Life에서 연결되며, 메뉴를 누르는 시점에 읽는다.
		patternActions = new PatternFileActions(this, model,
				() -> controller.pause(),
				() -> { controller.boardReplaced(); repaint(); });

		final Dimension PREFERRED_SIZE =
						new Dimension
						(  model.size() * DEFAULT_CELL_SIZE,
						   model.size() * DEFAULT_CELL_SIZE
						);

		addComponentListener
		(	new ComponentAdapter()
			{	public void componentResized(ComponentEvent e)
				{
					// Make sure that the cells fit evenly into the
					// total grid size so that each cell will be the
					// same size. For example, in a 64x64 grid, the
					// total size must be a multiple of 64.

					Rectangle bounds = getBounds();
					bounds.height /= model.size();
					bounds.height *= model.size();
					bounds.width  =  bounds.height;
					setBounds( bounds );
				}
			}
		);

		setBackground	( Color.white	 );
		setPreferredSize( PREFERRED_SIZE );
		setMaximumSize	( PREFERRED_SIZE );
		setMinimumSize	( PREFERRED_SIZE );
		setOpaque		( true			 );

		addMouseListener					//{=Universe.mouse}
		(	new MouseAdapter()
			{
				public void mousePressed(MouseEvent e)
				{
					// 픽셀 위치를 행·열로 바꾸고 격자 밖 입력은 무시한다.
					int pixelsPerCell = getWidth() / model.size();
					int boardPixels = pixelsPerCell * model.size();
					int x = e.getX();
					int y = e.getY();
					if (x < 0 || y < 0 || x >= boardPixels || y >= boardPixels) {
						return;
					}
					// 실행 중 클릭 처리는 현재 상태가 정한다.
					controller.cellClicked(y / pixelsPerCell, x / pixelsPerCell);
					repaint();
				}
			}
		);

		MenuSite.addLine( this, "Grid", "Clear",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	// 멈추고 비운 뒤 이력을 새로 시작하고 편집 상태로 돌아간다.
					controller.clearBoard();
					repaint();
				}
			}
		);

		MenuSite.addLine			// {=Universe.load.setup}
		(	this, "Grid", "Load",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	// 파일 선택 창이 떠 있는 동안 판이 바뀌지 않도록 먼저 멈춘다.
					controller.pause();
					patternActions.load();
				}
			}
		);

		MenuSite.addLine
		(	this, "Grid", "Store",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	// 저장할 판이 파일 선택 중에 바뀌지 않도록 먼저 멈춘다.
					controller.pause();
					patternActions.store();
				}
			}
		);

		patternActions.installPresetMenu(this);

		MenuSite.addLine
		(	this, "Grid", "Exit",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
		        {	System.exit(0);
		        }
			}
		);

		Clock.instance().addClockListener //{=Universe.clock.subscribe}
		(
			new Clock.Listener()
			{
				public void tick()
				{
					if (model.step()) {
						refreshNow();
					}
				}
			}
		);
	}

	public static Universe instance()
	{
		return theInstance;
	}

	/** 메뉴와 추가 기능은 같은 모델을 사용한다. */
	public BoardModel model()
	{
		return model;
	}

	/** 화면을 띄우기 전에 Life에서 한 번 연결한다. */
	public void setController(GameController controller)
	{
		this.controller = controller;
	}

	/** Override paint to ask the outermost Neighborhood
	 *  (and any subcells) to draw themselves recursively.
	 *  All knowledge of screen size is also encapsulated.
	 *  (The size is passed into the outermost <code>Cell</code>.)
	 */

	public void paint(Graphics g)
	{
		drawBoard(g, true);
	}

	private void drawBoard(Graphics g, boolean drawAll)
	{
		Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
		// 타이머가 보드를 바꾸는 도중에는 같은 보드를 그리지 않는다.
		synchronized (model) {
			boardToDraw.redraw(g, bounds, drawAll);
		}
	}

	/** Force a screen refresh by queing a request on
	 *  the Swing event queue. This is an example of the
	 *  Active Object pattern (not covered by the Gang of Four).
	 *  This method is called on every clock tick. Note that
	 *  the redraw() method on a given <code>Cell</code>
	 *  does nothing if the <code>Cell</code> doesn't
	 *  have to be refreshed.
	 */

	private void refreshNow()
	{	SwingUtilities.invokeLater
		(	new Runnable()
			{	public void run()
				{	Graphics g = getGraphics();
					if( g == null )		// Universe not displayable
						return;
					try
					{
						drawBoard(g, false); //{=Universe.redraw2}
					}
					finally
					{	g.dispose();
					}
				}
			}
		);
	}
}
