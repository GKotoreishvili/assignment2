// Board.java
package tetris;

import org.omg.Messaging.SYNC_WITH_TRANSPORT;

import java.awt.*;

/**
 CS108 Tetris Board.
 Represents a Tetris board -- essentially a 2-d grid
 of booleans. Supports tetris pieces and row clearing.
 Has an "undo" feature that allows clients to add and remove pieces efficiently.
 Does not do any drawing or have any idea of pixels. Instead,
 just represents the abstract 2-d board.
*/
public class Board	{
	// Some ivars are stubbed out for you:
	private int width;
	private int height;
	private int maxHeight;
	private int maxHeightBachUp;
	private int[] rowCount;
	private int[] columnCount;
	private int[] rowCountBackUp;
	private int[] columnCountBackUp;
	private boolean[][] gridBackUp;
	private boolean[][] grid;
	private Piece lastPiece;
	private int lX;
	private int lY;
	private boolean DEBUG = true;
	boolean committed;
	
	
	private boolean valid(int ind1, int ind2) {
		if (0 > ind1 || ind1 >= this.getHeight()) {
			return false;
		}
		if (0 > ind2 || ind2 >= this.getWidth()) {
			return false;
		}
		return true;
	}
	
	// Here a few trivial methods are provided:
	
	/**
	 Creates an empty board of the given width and height
	 measured in blocks.
	*/
	public Board(int width, int height) {
		
		this.width = width;
		this.height = height;
		maxHeight = maxHeightBachUp = 0;
		
		grid = new boolean[height][width];
		gridBackUp = new boolean[height][width];
		for (int i=0; i<height; i++) {
			for (int j=0; j<width; j++) {
				grid[i][j] = gridBackUp[i][j] = false;
			}
		}
		committed = false;
		
		rowCount = new int[height];
		rowCountBackUp = new int[height];
		columnCount = new int[width];
		columnCountBackUp = new int[width];
		for (int j=0; j<height; j++) {
			rowCount[j] = rowCountBackUp[j] = 0;
		}
		for (int j=0; j<width; j++) {
			columnCount[j] = columnCount[j] = 0;
		}
		
	}
	
	
	/**
	 Returns the width of the board in blocks.
	*/
	public int getWidth() {
		return width;
	}
	
	
	/**
	 Returns the height of the board in blocks.
	*/
	public int getHeight() {
		return height;
	}
	
	
	/**
	 Returns the max column height present in the board.
	 For an empty board this is 0.
	*/
	public int getMaxHeight() {
		
		return maxHeight;
	}
	
	
	/**
	 Checks the board for internal consistency -- used
	 for debugging.
	*/
	public void sanityCheck() {
		if (DEBUG) {
			
			int max = 0;
			for (int i=0; i<height; i++) {
				
				int cnt = 0;
				for (int j=0; j<width; j++) {
					
					if (grid[i][j]) cnt++;
				}
				
				if (cnt != rowCount[i]) throw new RuntimeException("description");
			}
			
			for (int i=0; i<width; i++) {
				
				int cnt = 0;
				for (int j=0; j<height; j++) {
					
					if (grid[i][j]) cnt++;
				}
				
				max = Math.max(max, cnt);
				if (cnt != columnCount[i]) throw new RuntimeException("description");
			}
			if (max != maxHeight) throw new RuntimeException("description");
		}
	}
	
	/**
	 Given a piece and an x, returns the y
	 value where the piece would come to rest
	 if it were dropped straight down at that x.
	 
	 <p>
	 Implementation: use the skirt and the col heights
	 to compute this fast -- O(skirt length).
	*/
	public int dropHeight(Piece piece, int x) {

		return getColumnHeight(x) + piece.getSkirt()[0];
	}
	
	
	/**
	 Returns the height of the given column --
	 i.e. the y value of the highest block + 1.
	 The height is 0 if the column contains no blocks.
	*/
	public int getColumnHeight(int x) {

		return columnCount[x];
	}
	
	
	/**
	 Returns the number of filled blocks in
	 the given row.
	*/
	public int getRowWidth(int y) {
		
		return rowCount[height - y - 1];
	}
	
	
	/**
	 Returns true if the given block is filled in the board.
	 Blocks outside of the valid width/height area
	 always return true.
	*/
	public boolean getGrid(int x, int y) {
		
		if (!valid(x, y)) return true;
		return grid[x][y];
	}
	
	
	public static final int PLACE_OK = 0;
	public static final int PLACE_ROW_FILLED = 1;
	public static final int PLACE_OUT_BOUNDS = 2;
	public static final int PLACE_BAD = 3;

	
	private int maxBody(int ind, Piece piece) {
		int val=0;
		TPoint[] body = piece.getBody();
		for (int i=0; i<body.length; i++) {
			if (body[i].x == ind && body[i].y > val) {
				System.out.println(val);
				val = body[i].y;
			}
		}
		return val+1;
	}

	private int calc(int ind, Piece piece) {
		int mn=piece.getHeight()+1, mx=0;
		TPoint[] body = piece.getBody();
		for (int i=0; i<body.length; i++) {
			if (body[i].y == ind) {
				mn = Math.min(mn, body[i].x);
				mx = Math.max(mx, body[i].x);
			}
		}
		return mx-mn+1;
	}

	/**
	 Attempts to add the body of a piece to the board.
	 Copies the piece blocks into the board grid.
	 Returns PLACE_OK for a regular placement, or PLACE_ROW_FILLED
	 for a regular placement that causes at least one row to be filled.
	 
	 <p>Error cases:
	 A placement may fail in two ways. First, if part of the piece may falls out
	 of bounds of the board, PLACE_OUT_BOUNDS is returned.
	 Or the placement may collide with existing blocks in the grid
	 in which case PLACE_BAD is returned.
	 In both error cases, the board may be left in an invalid
	 state. The client can use undo(), to recover the valid, pre-place state.
	*/
	public int place(Piece piece, int x, int y) {
		// flag !committed problem
		if (!committed) throw new RuntimeException("place commit problem");

		committed = false;
		x = height - x -1;
		y = width - y -1;
		int result = PLACE_OK;
		//System.out.println(x + " " + height);
		//System.out.println(y + " " + width);
		for (int i=0; i<piece.getBody().length; i++) {
			if (!valid(x - piece.getBody()[i].y, y + piece.getBody()[i].x)) {
				return PLACE_OUT_BOUNDS;
			}
		}
		
		for (int i=0; i<piece.getBody().length; i++) {
			int ind1 = x - piece.getBody()[i].y;
			int ind2 = y + piece.getBody()[i].x;
			if (grid[ind1][ind2]) {
				undo();
				return PLACE_BAD;
			}
			grid[ind1][ind2] = true;
		}

		for (int i=0; i<piece.getHeight(); i++) {
			rowCount[x-i] = calc(i, piece);
		}
		maxHeight = 0;
		for (int i=0; i<piece.getWidth(); i++) {
			columnCount[y+i] += maxBody(i, piece);
			maxHeight = Math.max(maxHeight, columnCount[y+i]);
		}

		for (int i=0; i<height; i++) {
			if (rowCount[i] == width) return PLACE_ROW_FILLED;
		}

		lastPiece = piece;
		lX = x;
		lY = y;
		System.out.println(PLACE_OK);
		return PLACE_OK;
	}
	
	
	/**
	 Deletes rows that are filled all the way across, moving
	 things above down. Returns the number of rows cleared.
	*/
	public int clearRows() {
		int rowsCleared=0, count=height-1, ind=height-1;

		while (count >= 0) {
			if (rowCount[ind] < width) {
				ind--;
				count--;
				continue;
			}
			rowsCleared++;
			for (int i=ind; i>0; i--) {
				rowCount[i] = rowCount[i-1];
				rowCount[i-1] = 0;
			}
			for (int i=ind; i>0; i--) {
				for (int j=0; j>=width; j++) {
					grid[i][j] = grid[i-1][j];
					grid[i-1][j] = false;
				}
			}
			count--;
		}
		for (int i=0; i<width; i++) {
			columnCount[i] -= rowsCleared;
		}
		maxHeight -= rowsCleared;
		sanityCheck();
		return rowsCleared;
	}
	
	
	/**
	 Reverts the board to its state before up to one place
	 and one clearRows();
	 If the conditions for undo() are not met, such as
	 calling undo() twice in a row, then the second undo() does nothing.
	 See the overview docs.
	*/
	public void undo() {

		for (int i=0; i<height; i++) {
			System.arraycopy(gridBackUp[i], 0, grid[i], 0, grid[i].length);
		}
		System.arraycopy(this.columnCountBackUp, 0, this.columnCount, 0, height);
		System.arraycopy(this.rowCountBackUp, 0, this.rowCount, 0, width);
		maxHeight = maxHeightBachUp;
		committed = false;
	}
	
	
	/**
	 Puts the board in the committed state.
	*/
	public void commit() {

		for (int i=0; i<height; i++) {
			System.arraycopy(grid[i], 0, gridBackUp[i], 0, grid[i].length);
		}
		System.arraycopy(this.columnCount, 0, this.columnCountBackUp, 0, width);
		System.arraycopy(this.rowCount, 0, this.rowCountBackUp, 0, height);
		maxHeightBachUp = maxHeight;
		committed = true;
	}


	
	/*
	 Renders the board state as a big String, suitable for printing.
	 This is the sort of print-obj-state utility that can help see complex
	 state change over time.
	 (provided debugging utility) 
	 */
	public String toString() {
		StringBuilder buff = new StringBuilder();
		for (int y = height-1; y>=0; y--) {
			buff.append('|');
			for (int x=0; x<width; x++) {
				if (getGrid(x,y)) buff.append('+');
				else buff.append(' ');
			}
			buff.append("|\n");
		}
		for (int x=0; x<width+2; x++) buff.append('-');
		return(buff.toString());
	}
}
