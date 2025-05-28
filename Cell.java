package it.polito.extgol;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
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

@Entity
public class Cell implements Evolvable, Interactable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "x", column = @Column(name = "cell_x", nullable = false)),
        @AttributeOverride(name = "y", column = @Column(name = "cell_y", nullable = false))
    })
    private Coord cellCoord;

    @Column(name = "is_alive", nullable = false)
    protected Boolean isAlive = false;

    @Column(name = "lifepoints", nullable = false)
    protected Integer lifepoints = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false, updatable = false)
    protected Board board;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false, updatable = false)
    protected Game game;

    @Transient
    protected List<Generation> generations = new ArrayList<>();

    @OneToOne(mappedBy = "cell", fetch = FetchType.LAZY)
    protected Tile tile;

    @Transient
    private CellType type = CellType.BASIC;

    @Transient
    private CellMood mood = CellMood.NAIVE;

    public Cell() {}

    public Cell(Coord tileCoord) {
        this.cellCoord = tileCoord;
        this.isAlive = false;
    }

    public Cell(Coord tileCoord, Tile t, Board b, Game g) {
        this.cellCoord = tileCoord;
        this.isAlive = false;
        this.tile = t;
        this.board = b;
        this.game = g;
    }

    public Cell(int x, int y, Board b, CellType type) {
        this.cellCoord = new Coord(x, y);
        this.board = b;
        this.type = type != null ? type : CellType.BASIC;
        this.mood = CellMood.NAIVE;
        this.isAlive = false;
    }

    @Override
    public Boolean evolve(int aliveNeighbors) {
        Boolean willLive = this.isAlive;

        if (aliveNeighbors > 3)
            willLive = false;
        else if (aliveNeighbors < 2)
            willLive = false;
        else if (!this.isAlive && aliveNeighbors == 3)
            willLive = true;

        return willLive;
    }

    public List<Tile> getNeighbors() {
        return tile != null ? List.copyOf(tile.getNeighbors()) : List.of();
    }

    public int countAliveNeighbors() {
        int count = 0;
        if (tile != null) {
            for (Tile t : tile.getNeighbors()) {
                if (t.getCell() != null && t.getCell().isAlive())
                    count++;
            }
        }
        return count;
    }

    void addGeneration(Generation gen) {
        generations.add(gen);
    }

    public List<Generation> getGenerations() {
        return List.copyOf(generations);
    }

    public int getX() {
        return this.cellCoord.getX();
    }

    public int getY() {
        return this.cellCoord.getY();
    }

    public Coord getCoordinates() {
        return this.cellCoord;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean isAlive) {
        this.isAlive = isAlive;
    }

    @Override
    public String toString() {
        return getX() + "," + getY();
    }

    public int getLifePoints() {
        return lifepoints;
    }

    public void setLifePoints(int lifePoints) {
        this.lifepoints = lifePoints;
    }

    @Override
    public void interact(Cell otherCell) {
        if (otherCell == null) return;

        if (this.mood == CellMood.VAMPIRE) {
            int stolen = Math.min(1, otherCell.getLifePoints());
            otherCell.setLifePoints(otherCell.getLifePoints() - stolen);
            this.setLifePoints(this.getLifePoints() + stolen);
        } else if (this.mood == CellMood.HEALER) {
            this.setLifePoints(Math.max(0, this.getLifePoints() - 1));
            otherCell.setLifePoints(otherCell.getLifePoints() + 1);
        }
    }

    public void setType(CellType t) {
        if (t != null) this.type = t;
    }

    public void setMood(CellMood mood) {
        if (mood != null) this.mood = mood;
    }

    public CellMood getMood() {
        return mood;
    }

    public CellType getType() {
        return type;
    }

    public Tile getTile() {
        return this.tile;
    }
}
