package it.polito.extgol;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

/**
 * Entity representing a single square on the Game of Life board.
 * Holds coordinate position, occupying Cell, and link back to its Board.
 */
@Entity
public class Tile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Coordinates of the tile on the board. */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "x",
            column = @Column(name = "tile_x", nullable = false)),
        @AttributeOverride(name = "y",
            column = @Column(name = "tile_y", nullable = false))
    })
    private Coord tileCoord;

    /** Reference to the board containing this tile. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    /** Reference to the owning game. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false, updatable = false)
    private Game game;

    /**
     * The cell occupying this tile.
     */
    @OneToOne(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY
    )
    @JoinColumn(name = "cell_id", nullable = false, unique = true)
    private Cell cell;

    /** Neighboring tiles for interactions. Not persisted. */
    @Transient
    private Set<Tile> neighbors = new HashSet<Tile>();

    /**
     * Default constructor required by JPA.
     */
    public Tile() {
    }

    /**
     * Constructs a tile at the given coordinates,
     * and the respective cell initialized as not alive
     * @param x column index (0-based)
     * @param y row index (0-based)
     */
    public Tile(int x, int y, Board b, Game g) {
        this.tileCoord = new Coord(x,y);
        this.board = b;
        this.game = g;
        this.cell = new Cell(this.tileCoord, this, b, g);
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public Cell getCell() {
        return cell;
    }

    public void setCell(Cell cell) {
        this.cell = cell;
    }

    public boolean hasCell() {
        return cell != null && cell.isAlive();
    }

    public void initializeNeighbors(Set<Tile> neighborsList) {
        this.neighbors = neighborsList;
    }

    public Set<Tile> getNeighbors() {
        return this.neighbors;
    }

    public Long getId() {
        return id;
    }

    public Coord getTileCoord() {
        return this.tileCoord;
    }

    public int getX() {
        return this.tileCoord.getX();
    }

    public int getY() {
        return this.tileCoord.getY();
    }

    /**
     * Life point modifier is 0 for a normal tile.
     * Interactable tiles will override this.
     */
    public Integer getLifePointModifier() {
        return 0;
    }
}
