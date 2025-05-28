package it.polito.extgol;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

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

    @Override
    public Boolean evolve(int aliveNeighbors) {
        Boolean willLive = this.isAlive;

        if (aliveNeighbors > 3 || aliveNeighbors < 2)
            willLive = false;
        else if (!this.isAlive && aliveNeighbors == 3)
            willLive = true;

        return willLive;
    }

    public List<Tile> getNeighbors() {
        if (tile != null && tile.getNeighbors() != null) {
            return List.copyOf(tile.getNeighbors());
        }
        return List.of();
    }

    public int countAliveNeighbors() {
        int count = 0;
        if (tile != null) {
            for (Tile t : tile.getNeighbors()) {
                Cell neighbor = t.getCell();
                if (neighbor != null && neighbor.isAlive()) {
                    count++;
                }
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
        return getX() + "," + getY() + " [" + mood + ", " + type + "]";
    }

    public int getLifePoints() {
        return lifepoints;
    }

    public void setLifePoints(int lifePoints) {
        this.lifepoints = Math.max(0, lifePoints);
    }

    @Override
    public void interact(Cell otherCell) {
        if (otherCell == null) return;

        switch (this.mood) {
            case VAMPIRE -> {
                int stolen = Math.min(1, Math.max(0, otherCell.getLifePoints()));
                otherCell.setLifePoints(otherCell.getLifePoints() - stolen);
                this.setLifePoints(this.getLifePoints() + stolen);
            }
            case HEALER -> {
                if (this.lifepoints > 0) {
                    this.setLifePoints(this.getLifePoints() - 1);
                    otherCell.setLifePoints(otherCell.getLifePoints() + 1);
                }
            }
            case NAIVE -> {
                // No action
            }
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

    public void setTile(Tile tile) {
        this.tile = tile;
    }

    public Tile getTile() {
        return tile;
    }
}
