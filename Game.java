package it.polito.extgol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.*;

@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToOne(
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(name = "board_id", nullable = false, unique = true)
    private Board board;

    @OneToMany(
        mappedBy = "game",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @OrderColumn(name = "generation_index")
    private List<Generation> generations = new ArrayList<>();

    @Transient
    private Map<Integer, EventType> eventMap = new HashMap<>();

    protected Game() {
    }

    protected Game(String name) {
        this.name = name;
    }

    public Game(String name, int width, int height) throws ExtendedGameOfLifeException {
        if (name == null || name.trim().isEmpty())
            throw new ExtendedGameOfLifeException("Game name cannot be null or empty");
        if (width <= 0 || height <= 0)
            throw new ExtendedGameOfLifeException("Board dimensions must be positive");

        this.name = name;
        this.board = new Board(width, height, this);
        Generation.createInitial(this, board);
    }

    public static Game create(String name, int width, int height) throws ExtendedGameOfLifeException {
        Game game = new Game(name);
        Board board = new Board(width, height, game);
        game.setBoard(board);
        Generation.createInitial(game, board);
        return game;
    }

    public static Game createExtended(String name, int width, int height) throws ExtendedGameOfLifeException {
        Game game = new Game(name);
        Board board = Board.createExtended(width, height, game);
        game.setBoard(board);
        Generation.createInitial(game, board);
        return game;
    }

    public void addGeneration(Generation generation) throws ExtendedGameOfLifeException {
        if (generation == null) {
            throw new ExtendedGameOfLifeException("Cannot add null generation");
        }
        generation.setGame(this);
        generations.add(generation);
    }

    public void addGeneration(Generation generation, Integer step) throws ExtendedGameOfLifeException {
        if (generation == null) {
            throw new ExtendedGameOfLifeException("Cannot add null generation");
        }
        if (step == null || step < 0 || step > generations.size()) {
            throw new ExtendedGameOfLifeException("Invalid generation step index: " + step);
        }
        generation.setGame(this);
        generations.add(step, generation);
    }

    public void clearGenerations() {
        generations.clear();
    }

    public List<Generation> getGenerations() {
        return generations;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws ExtendedGameOfLifeException {
        if (name == null || name.trim().isEmpty()) {
            throw new ExtendedGameOfLifeException("Game name cannot be null or empty");
        }
        this.name = name;
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board b) throws ExtendedGameOfLifeException {
        if (b == null) {
            throw new ExtendedGameOfLifeException("Board cannot be null");
        }
        this.board = b;
    }

    public Generation getStart() throws ExtendedGameOfLifeException {
        if (generations.isEmpty()) {
            throw new ExtendedGameOfLifeException("No generations available");
        }
        return generations.get(0);
    }

    public void unrollEvent(EventType event, Cell cell) throws ExtendedGameOfLifeException {
        if (cell == null) throw new ExtendedGameOfLifeException("Cell cannot be null");
        if (event == null) throw new ExtendedGameOfLifeException("Event cannot be null");

        switch (event) {
            case CATACLYSM:
                cell.setLifePoints(0);
                break;
            case FAMINE:
                cell.setLifePoints(Math.max(0, cell.getLifePoints() - 1));
                break;
            case BLOOM:
                cell.setLifePoints(cell.getLifePoints() + 2);
                break;
            case BLOOD_MOON:
                if (cell.getMood() == CellMood.VAMPIRE) {
                    Tile tile = cell.getTile();
                    if (tile != null) {
                        for (Tile neighbor : tile.getNeighbors()) {
                            Cell neighborCell = neighbor.getCell();
                            if (neighborCell == null) continue;
                            CellMood mood = neighborCell.getMood();
                            if (mood == CellMood.NAIVE || mood == CellMood.HEALER) {
                                int stolen = Math.min(1, neighborCell.getLifePoints());
                                neighborCell.setLifePoints(neighborCell.getLifePoints() - stolen);
                                cell.setLifePoints(cell.getLifePoints() + stolen);
                                neighborCell.setMood(CellMood.VAMPIRE);
                            }
                        }
                    }
                }
                break;
            case SANCTUARY:
                if (cell.getMood() == CellMood.HEALER) {
                    cell.setLifePoints(cell.getLifePoints() + 1);
                } else if (cell.getMood() == CellMood.VAMPIRE) {
                    cell.setMood(CellMood.NAIVE);
                }
                break;
            default:
                throw new ExtendedGameOfLifeException("Unknown event type: " + event);
        }
    }

    public void setMood(CellMood mood, List<Coord> targetCoordinates) throws ExtendedGameOfLifeException {
        if (board == null) {
            throw new ExtendedGameOfLifeException("Board not initialized");
        }
        if (targetCoordinates == null || targetCoordinates.isEmpty()) {
            throw new ExtendedGameOfLifeException("Target coordinates cannot be null or empty");
        }

        for (Coord coord : targetCoordinates) {
            Tile tile = board.getTile(coord);
            if (tile == null) {
                throw new ExtendedGameOfLifeException("No tile found at coordinate: " + coord);
            }
            Cell cell = tile.getCell();
            if (cell == null) {
                throw new ExtendedGameOfLifeException("No cell on tile at coordinate: " + coord);
            }
            cell.setMood(mood);
        }
    }

    public Map<Integer, EventType> getEventMap() {
        return eventMap;
    }

    public void scheduleEvent(int generationIndex, EventType event) throws ExtendedGameOfLifeException {
        if (generationIndex < 0 || generationIndex > generations.size()) {
            throw new ExtendedGameOfLifeException("Invalid generation index for event");
        }
        if (event == null) {
            throw new ExtendedGameOfLifeException("Event type cannot be null");
        }
        eventMap.put(generationIndex, event);
    }

    public EventType getScheduledEvent(int generationIndex) {
        return eventMap.get(generationIndex);
    }
}
