package it.polito.extgol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "generation", uniqueConstraints = @UniqueConstraint(columnNames = { "game_id", "step" }))
public class Generation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer step;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "generation_state", joinColumns = {
            @JoinColumn(name = "generation_id", referencedColumnName = "id")
    })
    @MapKeyJoinColumn(name = "cell_id")
    @Column(name = "is_alive", nullable = false)
    private Map<Cell, Boolean> cellAlivenessStates = new HashMap<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "generation_energy", joinColumns = @JoinColumn(name = "generation_id"))
    @MapKeyJoinColumn(name = "cell_id")
    @Column(name = "life_points", nullable = false)
    private Map<Cell, Integer> energyStates = new HashMap<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "generation_mood", joinColumns = @JoinColumn(name = "generation_id"))
    @MapKeyJoinColumn(name = "cell_id")
    @Column(name = "mood", nullable = false)
    private Map<Cell, CellMood> moodStates = new HashMap<>();

    protected Generation() {
    }

    protected Generation(Game game, Board board, int step) {
        this.game = Objects.requireNonNull(game, "Game cannot be null");
        this.board = Objects.requireNonNull(board, "Board cannot be null");
        this.step = step;
    }

    protected Generation(Game game, int step) {
        this.game = Objects.requireNonNull(game, "Game cannot be null");
        this.step = step;
    }

    public void setType(List<Coord> coords, CellType type) {
        Objects.requireNonNull(coords, "Coordinates list cannot be null");
        Objects.requireNonNull(type, "CellType cannot be null");

        for (Coord c : coords) {
            Tile tile = board.getTile(c);
            if (tile == null)
                throw new IllegalArgumentException("Invalid coordinate: " + c);

            Cell cell = tile.getCell();
            if (cell == null) {
                cell = new Cell(c.getX(), c.getY(), board, type);
                tile.setCell(cell);
            } else {
                cell.setType(type);
            }
            cell.setAlive(true);
        }
        this.snapCells();
    }

    public static Generation createInitial(Game game, Board board) {
        Objects.requireNonNull(game, "Game cannot be null");
        Objects.requireNonNull(board, "Board cannot be null");

        game.clearGenerations();
        Generation init = new Generation(game, board, 0);

        for (Tile t : board.getTiles()) {
            if (t.getCell() != null)
                t.getCell().setAlive(false);
        }

        init.snapCells();
        game.addGeneration(init, 0);
        return init;
    }

    public static Generation createInitial(Game game, Board board, List<Coord> aliveCells) {
        Objects.requireNonNull(game, "Game cannot be null");
        Objects.requireNonNull(board, "Board cannot be null");
        Objects.requireNonNull(aliveCells, "aliveCells cannot be null");

        game.clearGenerations();

        for (Tile t : board.getTiles()) {
            if (t.getCell() != null)
                t.getCell().setAlive(false);
        }

        Generation init = new Generation(game, board, 0);

        for (Coord c : aliveCells) {
            Tile tile = board.getTile(c);
            if (tile == null)
                throw new IllegalArgumentException("Invalid coordinate: " + c);
            Cell cell = tile.getCell();
            if (cell != null) {
                cell.setAlive(true);
            } else {
                cell = new Cell(c.getX(), c.getY(), board, CellType.NAIVE);
                cell.setAlive(true);
                tile.setCell(cell);
            }
        }

        init.snapCells();
        game.addGeneration(init, 0);
        return init;
    }

    public static Generation createInitial(Game game, Board board, Map<Coord, CellType> cellTypesMap) {
        Objects.requireNonNull(game, "Game cannot be null");
        Objects.requireNonNull(board, "Board cannot be null");
        Objects.requireNonNull(cellTypesMap, "cellTypesMap cannot be null");

        game.clearGenerations();

        for (Tile t : board.getTiles()) {
            if (t.getCell() != null) {
                t.getCell().setAlive(false);
                t.getCell().setType(CellType.NAIVE);
            }
        }

        Generation init = new Generation(game, board, 0);

        for (Map.Entry<Coord, CellType> entry : cellTypesMap.entrySet()) {
            Coord c = entry.getKey();
            CellType type = entry.getValue();
            Tile tile = board.getTile(c);
            if (tile == null)
                throw new IllegalArgumentException("Invalid coordinate: " + c);
            Cell cell = tile.getCell();
            if (cell == null) {
                cell = new Cell(c.getX(), c.getY(), board, type);
                tile.setCell(cell);
            } else {
                cell.setType(type);
            }
            cell.setAlive(true);
        }

        init.snapCells();
        game.addGeneration(init, 0);
        return init;
    }

    public static Generation createNextGeneration(Generation prev) {
        Objects.requireNonNull(prev, "Previous generation cannot be null");

        Generation next = new Generation(prev.getGame(), prev.getBoard(), prev.getStep() + 1);

        next.snapCells();
        prev.getGame().addGeneration(next, prev.getStep() + 1);
        return next;
    }

    public Map<Cell, Boolean> snapCells() {
        cellAlivenessStates.clear();
        energyStates.clear();
        moodStates.clear();

        for (Tile tile : board.getTiles()) {
            Cell cell = tile.getCell();
            if (cell == null) {
                throw new IllegalStateException("Each tile should hold a cell!");
            }
            cellAlivenessStates.put(cell, cell.isAlive());
            energyStates.put(cell, cell.getLifePoints());
            moodStates.put(cell, cell.getMood());
        }
        return Map.copyOf(cellAlivenessStates);
    }

    public Set<Cell> getAliveCells() {
        return cellAlivenessStates.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public void setState(List<Coord> coords, boolean aliveness) {
        Objects.requireNonNull(coords, "Coordinates list cannot be null");

        for (Coord c : coords) {
            Tile tile = board.getTile(c);
            if (tile == null)
                throw new IllegalArgumentException("Invalid coordinate: " + c);

            Cell cell = tile.getCell();
            if (cell == null)
                throw new IllegalStateException("Tile at " + c + " has no cell");

            cell.setAlive(aliveness);
        }
        this.snapCells();
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public int getStep() {
        return step;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = Objects.requireNonNull(game, "Game cannot be null");
    }

    public Board getBoard() {
        return board;
    }

    public Map<Cell, Integer> getEnergyStates() {
        return Map.copyOf(energyStates);
    }

    public void setEnergyStates(Map<Cell, Integer> energyStates) {
        this.energyStates = new HashMap<>(Objects.requireNonNull(energyStates, "energyStates cannot be null"));
    }

    public Map<Cell, Boolean> getCellAlivenessStates() {
        return Map.copyOf(cellAlivenessStates);
    }

    public void setCellAlivenessStates(Map<Cell, Boolean> cellAlivenessStates) {
        this.cellAlivenessStates = new HashMap<>(Objects.requireNonNull(cellAlivenessStates, "cellAlivenessStates cannot be null"));
    }

    public Map<Cell, CellMood> getMoodStates() {
        return Map.copyOf(moodStates);
    }

    public void setMoodStates(Map<Cell, CellMood> moodStates) {
        this.moodStates = new HashMap<>(Objects.requireNonNull(moodStates, "moodStates cannot be null"));
    }
}
