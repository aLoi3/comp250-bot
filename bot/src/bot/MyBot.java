package bot;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import rts.*;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

public class MyBot extends AbstractionLayerAI {    
    private UnitTypeTable utt;
    private UnitType worker;
    private UnitType archer;
    private UnitType light;
    private UnitType heavy;
    private UnitType barracks;
    private UnitType base;
    private boolean isOnTop;
    
    public MyBot(UnitTypeTable utt) {
        super(new AStarPathFinding());
        this.utt = utt;
        worker = utt.getUnitType("Worker");
        archer = utt.getUnitType("Ranged");
        light = utt.getUnitType("Light");
        heavy = utt.getUnitType("Heavy");
        barracks = utt.getUnitType("Barracks");
        base = utt.getUnitType("Base");
    }
    

    @Override
    public void reset() 
    {
    }

    
    @Override
    public AI clone() {
        return new MyBot(utt);
    }
   
    
    @Override
    /**
     * Called every tick and is the main body of the AI
     */
    public PlayerAction getAction(int player, GameState gs) {
    	// Get the physical game state
        PhysicalGameState pgs = gs.getPhysicalGameState();
        // Get our player
        Player p = gs.getPlayer(player);
        
        // Make a list of worker units
        List<Unit> workers = new LinkedList<Unit>();
        // Loop through every unit in the game
        for (Unit unit : pgs.getUnits()) 
        {
        	// If it's player's base unit, call a base behavior
        	if(unit.getType() == base 
        			&& unit.getPlayer() == player 
        			&& gs.getActionAssignment(unit) == null)
        	{
        		BaseBehavior(unit, p, pgs);
        	}
        	
        	// If it's player's barracks unit, call a barracks behavior
        	else if(unit.getType() == barracks 
        			&& unit.getPlayer() == player 
        			&& gs.getActionAssignment(unit) == null)
        	{
        		BarracksBehavior(unit, p, pgs);
        	}
        	
        	// If it's player's light or heavy unit, call a light and heavy behavior
        	else if(unit.getType() == light || unit.getType() == heavy 
        			&& unit.getPlayer() == player 
        			&& gs.getActionAssignment(unit) == null)
        	{
        		MeleeBehavior(unit, p, gs);
        	}
        	
        	// If it's player's ranged unit, call a ranged behavior
        	else if(unit.getType() == archer 
        			&& unit.getPlayer() == player 
        			&& gs.getActionAssignment(unit) == null)
        	{
        		RangedBehavior(unit, p, gs);
        	}
        	
        	// If it's player's worker unit, add to the list
        	else if(unit.getPlayer() == player && unit.getType().canHarvest)
        	{
        		workers.add(unit);
        	}
        }
        // Call a workers behavior
        WorkerBehavior(workers, p, gs);
        
        // Return all the actions of my player to simulate in the game
        return translateActions(player, gs);
    }
    
    /**
     * Looks for the closest enemy to attack
     * @param pgs Physical Game State
     * @param p Player
     * @param u Unit
     * @return Unit - closest enemy to the passed unit
     */
    public Unit GetClosestEnemy(PhysicalGameState pgs, Player p, Unit u)
    {
    	Unit closestEnemy = null;
    	int closestDistance = 0;
    	// Loop through every unit in the game
    	for(Unit u2:pgs.getUnits())
    	{
    		// Check if the unit is enemy's and not a resource
    		if(u2.getPlayer() >= 0 && u2.getPlayer() != p.getID())
    		{
    			// Calculate it's distance to my unit
    			int distance = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
    			// If there is no unit still assigned or the new distance is smaller than the previous one's - assign a new closest enemy
    			if(closestEnemy == null || distance < closestDistance)
    			{
    				closestEnemy = u2;
    				closestDistance = distance;
    			}
    		}
    	}
    	// Return closest enemy unit
    	return closestEnemy;
    }
    
    /**
     * Base behavior. Trains workers
     * @param u Unit - Base
     * @param p Player
     * @param pgs Physical Game State
     */
    public void BaseBehavior(Unit u, Player p, PhysicalGameState pgs)
    {
    	int nWorkers = 0;
    	int nBarracks = 0;
    	int workerCount = 5;
    	int multiplier = mapMultiplier(pgs);
    	
    	// Loop through every unit in the game and increment my workers and barracks
    	for(Unit u2 : pgs.getUnits())
    	{
    		if(u2.getType() == worker && u2.getPlayer() == p.getID())
    		{
    			nWorkers++;
    		}
    		
    		if(u2.getType() == barracks && u2.getPlayer() == p.getID())
    		{
    			nBarracks++;
    		}
    	}
    	
    	// Multiply the worker unit count to be produced depending on the map size
    	if (multiplier > 0)
    	{
    		workerCount = workerCount + (2 * multiplier);
    	}
    	
    	// Train my workers if possible
    	if(nWorkers < workerCount && p.getResources() >= worker.cost)
    	{
    		if(nWorkers < 2 || nBarracks >= 1 || p.getResources() >= 5)
    		{
    			train(u, worker);
    		}
    	}
    }
    
    /**
     * Barracks behavior. Trains Units
     * @param u Unit - barracks
     * @param p Player
     * @param pgs Physical Game State
     */
    public void BarracksBehavior(Unit u, Player p, PhysicalGameState pgs)
    {
    	// Train Ranged
    	TrainTroops(archer, worker, u, p, pgs, 3);
    	TrainTroops(archer, heavy, u, p, pgs, 1);
    	// Train Light
    	TrainTroops(light, archer, u, p, pgs, 2);
    	// Train Heavy
    	TrainTroops(heavy, light, u, p, pgs, 2);
    }
    
    /**
     * Trains certain type of unit that is passed in. The amount of units trained depends on the amount of specific type of units enemy has
     * @param troopToTrain Unit Type to Train
     * @param enemyTroop Unit Type to Compare
     * @param u Unit - barracks
     * @param p Player
     * @param pgs Physical Game State
     * @param enemyAmount Number of Certain Type of Enemy Units
     */
    public void TrainTroops(UnitType troopToTrain, UnitType enemyTroop, Unit u, Player p, PhysicalGameState pgs, int enemyAmount)
    {
    	// Get the amount of enemy's specific unit count
    	int enemyTroopCount = EnemyUnitCount(p, pgs, enemyTroop, troopToTrain);
    	int myRanged = 0;
    	// Get all my ranged units
    	for(Unit u2 : pgs.getUnits())
    	{
    		if (u2.getType() == archer && u2.getPlayer() == p.getID())
    		{
    			myRanged++;
    		}
    	}
    	// Train units depending on the enemy unit amount
    	if (enemyTroopCount >= enemyAmount)
    	{
    		if(p.getResources() >= troopToTrain.cost)
    		{
    			train(u, troopToTrain);
    		}
    	}
    	// Else, train up to 3 ranged units
    	else
    	{
    		if(p.getResources() >= archer.cost && myRanged < 3)
    		{
    			train(u, archer);
    		}
    	}
    }
    
    /**
     * This function checks how many certain typed enemy units there are. It scales down with the number of my units of certain type
     * @param p Player
     * @param pgs Physical Game State
     * @param enemy Enemy Unit Type
     * @param ally Allied Unit Type
     * @return Number of certain type enemies
     */
    public int EnemyUnitCount(Player p, PhysicalGameState pgs, UnitType enemy, UnitType ally)
    {
    	int myUnitCount = 0;
    	int enemyUnitCount = 0;
    	
    	for(Unit u2 : pgs.getUnits())
    	{
    		// Get my unit count. The type is passed in
    		if(u2.getPlayer() == p.getID() && u2.getType() == ally)
    		{
    			myUnitCount++; 
    		}
    		
    		// Get enemy unit count. The type is passed in
    		if(u2.getPlayer() != p.getID() && u2.getType() == enemy)
    		{
    			enemyUnitCount++;
    		}
    	}
    	
    	// Get enemy worker unit count
    	if(myUnitCount > 0 && enemy == worker)
    	{
    		enemyUnitCount = enemyUnitCount - (myUnitCount * 3);
    	}
    	
    	// Get enemy light unit count
    	if(myUnitCount > 0 && enemy == light)
    	{
    		enemyUnitCount = enemyUnitCount - (myUnitCount * 2);
    	}
    	
    	// Get enemy heavy unit count
    	if(myUnitCount > 0 && enemy == heavy)
    	{
    		enemyUnitCount = enemyUnitCount - (myUnitCount * 1);
    	}
    	
    	// Get enemy ranged unit count
    	if(myUnitCount > 0 && enemy == archer)
    	{
    		enemyUnitCount = enemyUnitCount - (myUnitCount * 2);
    	}
    	
    	return enemyUnitCount;
    }
    
    /**
     * Melee unit behavior. Attack the closest enemy if reachable
     * @param u Unit
     * @param p Player
     * @param gs Game State
     */
    public void MeleeBehavior(Unit u, Player p, GameState gs)
    {
    	PhysicalGameState pgs = gs.getPhysicalGameState();
    	// Get the closest enemy
    	Unit closestEnemy = GetClosestEnemy(pgs, p, u);
    	// Check if there is an enemy
    	if(closestEnemy != null)
    	{
    		// Check if can move there
    		boolean doesPathExists = DoesPathExists(closestEnemy, u, gs);
    		// If true - attack
    		if (doesPathExists)
    		{
    			attack(u, closestEnemy);
    		}
    		// If false - stack together
    		else
    		{
    			// List of my light and heavy units
    			List<Unit> units = new LinkedList<Unit>();
    			for (Unit u2 : pgs.getUnits())
				{
					if (u2.getPlayer() == p.getID() && u2.getType() == light || u2.getType() == heavy)
					{
						units.add(u2);
					}
				}
    			// Stack them together
    			Stack(gs, units);
    		}
    	}
    }
    
    /**
     * Ranged unit behavior. Attacks the closest enemy if can reach him
     * @param u Unit
     * @param p Plater
     * @param gs Game State
     */
    public void RangedBehavior(Unit u, Player p, GameState gs)
    {
    	PhysicalGameState pgs = gs.getPhysicalGameState();
    	// Get the closest enemy
    	Unit closestEnemy = GetClosestEnemy(pgs, p, u);
    	// Check if there is an enemy
    	if (closestEnemy != null)
    	{
    		// Get the distance to the enemy
        	int distance = Math.abs(closestEnemy.getX() - u.getX()) + Math.abs(closestEnemy.getY() - u.getY());
    		// Check if can move there
        	boolean doesPathExists = DoesPathExists(closestEnemy, u, gs);
			// If true - attacks
    		if (doesPathExists || distance < 3)
    		{
    			attack(u, closestEnemy);
    		}
    		// If false - stack together
    		else
    		{
    			// List of my ranged units
    			List<Unit> units = new LinkedList<Unit>();
    			for (Unit u2 : pgs.getUnits())
				{
					if (u2.getPlayer() == p.getID() && u2.getType() == archer)
					{
						units.add(u2);
					}
				}
    			// Stack them together
    			Stack(gs, units);
    		}
    	}
    }
    
    /**
     * Worker behavior. Consists of harvesters, builders and attackers
     * @param workers List of workers
     * @param p Player
     * @param gs Game State
     */
    public void WorkerBehavior(List<Unit> workers, Player p, GameState gs)
    {
    	PhysicalGameState pgs = gs.getPhysicalGameState();
    	
    	int nBases = 0;
    	int nBarracks = 0;
    	Unit myBase = null;
    	
    	int basesToBuild = 1;
    	int barracksToBuild = 1;
    	int harvestersNeeded = 2;
    	
    	// Get the multiplier
    	int multiplier = mapMultiplier(pgs);

    	// List of different worker types - builders, harvesters and attacker
    	List<Unit> builders = new LinkedList<Unit>();
    	List<Unit> harvesters = new LinkedList<Unit>();
    	List<Unit> attackers = new LinkedList<Unit>();
    	// List of closest resources to the base
    	List<Unit> closeResources = new LinkedList<Unit>();
    	
    	// If there are no workers passed it, return
    	if (workers.isEmpty())
    	{
    		return;
    	}
    	
    	// Get the number of my barracks and bases
    	for (Unit u2 : pgs.getUnits())
    	{    		
    		if(u2.getType() == base && u2.getPlayer() == p.getID())
    		{
    			nBases++;
    			// Assign my base
    			myBase = u2;
    		}
    		
    		if(u2.getType() == barracks && u2.getPlayer() == p.getID())
    		{
    			nBarracks++;
    		}
    	}
    	
    	// Check whether we are at the top or at the bottom
    	if(myBase != null)
    	{
	    	if(myBase.getX() < pgs.getWidth() / 2)
	    	{
	    		isOnTop = true;
	    	}
	    	else
	    	{
	    		isOnTop = false;
	    	}
    	}
    	
    	// Loop through every unit in the game
    	for(Unit u2 : pgs.getUnits())
    	{
    		// Check if it's a resource
    		if(u2.getType().isResource && myBase != null)
    		{
    			// Check the distance from the resource to the base
    			int distance = Math.abs(u2.getX() - myBase.getX()) + Math.abs(u2.getY() - myBase.getY());
    			if(distance < 5)
    			{
    				// If close enough - add to the close resources list
    				closeResources.add(u2);
    			}
    		}
    	}
    	
    	// Assign my workers for different jobs
    	for (Unit u2 : workers)
    	{
	    	if (harvesters.size() < closeResources.size() && harvesters.size() < harvestersNeeded)
	    	{
	    		harvesters.add(u2);
	    	}
	    	else if (builders.size() < 1)
	    	{
	    		builders.add(u2);
	    	}
	    	else
	    	{
	    		attackers.add(u2);
	    	}
    	}
    	
    	List<Integer> reservedPositions = new LinkedList<Integer>();
    	
    	// Build bases if we need and can
    	if (nBases < basesToBuild && !builders.isEmpty())
    	{
    		if(p.getResources() >= base.cost)
    		{
    			Unit u = builders.remove(0);
    			buildIfNotAlreadyBuilding(u, base, u.getX(), u.getY(), reservedPositions, p, pgs);
    		}	
    	}
    	// Build barracks if we need and can
    	barracksToBuild += multiplier;
    	if (nBarracks < barracksToBuild && !builders.isEmpty())
    	{
			// Make several barracks depending on the map size
    		if(p.getResources() >= barracks.cost && myBase != null)
    		{
				Unit u = builders.remove(0);
			
    			if(isOnTop)
        		{
    				if(nBarracks == 0)
    				{
    					buildIfNotAlreadyBuilding(u, barracks, myBase.getX() + 1, myBase.getY() + 3, reservedPositions, p, pgs);
    				}
    				if (nBarracks == 1)
    				{
    					buildIfNotAlreadyBuilding(u, barracks, myBase.getX() + 1, myBase.getY() + 3, reservedPositions, p, pgs);
    				}
    				else
    				{
    					buildIfNotAlreadyBuilding(u, barracks, myBase.getX() + 3, myBase.getY() + 1, reservedPositions, p, pgs);
    				}
        		}
    			else
    			{
    				if(nBarracks == 0)
    				{
    					buildIfNotAlreadyBuilding(u, barracks, myBase.getX() - 2, myBase.getY() - 2, reservedPositions, p, pgs);
    				}
    				if (nBarracks == 1)
    				{
    					buildIfNotAlreadyBuilding(u, barracks, myBase.getX() - 1, myBase.getY() - 3, reservedPositions, p, pgs);
    				}
    				else
    				{
    					buildIfNotAlreadyBuilding(u, barracks, myBase.getX() - 3, myBase.getY() - 1, reservedPositions, p, pgs);
    				}
    			}
    		}
    	}
    	
    	// If we have enough barracks, assign builders to be attackers
    	if (nBarracks == barracksToBuild)
    	{
    		if(!builders.isEmpty())
    		{
    			attackers.addAll(builders);
    		}
    	}
    	
    	// Make the harvest all the materials!
    	if(!harvesters.isEmpty())
    	{
	        for (Unit u : harvesters) 
	        {
	            Unit closestBase = null;
	            Unit closestResource = null;
	            int closestDistance = 0;
	            
	            // If there are no resources nearby - ATTACK!
	            if(closeResources.isEmpty())
	            {
	            	attackers.addAll(harvesters);
	            	harvesters.removeAll(harvesters);
	            }
	            // Otherwise just harvest different resources
	            else
	            {
		            for (int i=0; i < closeResources.size(); i++) 
		            {
		                if (closeResources.get(i).getType().isResource) 
		                {
		                    int d = Math.abs(closeResources.get(i).getX() - u.getX()) + Math.abs(closeResources.get(i).getY() - u.getY());
		                    if (closestResource == null || d < closestDistance) 
		                    {
		                       closestResource = closeResources.remove(i);
		                       closestDistance = d;
		                    }
		                }
		            }
		            closestDistance = 0;
		            for (Unit u2 : pgs.getUnits()) 
		            {
		                if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) 
		                {
		                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
		                    if (closestBase == null || d < closestDistance) 
		                    {
		                        closestBase = u2;
		                        closestDistance = d;
		                    }
		                }
		            }
		            if (closestResource != null && closestBase != null) 
		            {
		                AbstractAction aa = getAbstractAction(u);
		                if (aa instanceof Harvest) 
		                {
		                    Harvest h_aa = (Harvest)aa;
		                    if (h_aa.getTarget() != closestResource || h_aa.getBase()!=closestBase) harvest(u, closestResource, closestBase);
		                } 
		                else 
		                {
		                    harvest(u, closestResource, closestBase);
		                }
		            }
	            }
	        }
    	}
    	
    	// Check if there are attackers in the team
    	if(!attackers.isEmpty())
    	{
    		for (Unit u : attackers)
    		{
    			// Get the closest enemy
	    		Unit closestEnemy = GetClosestEnemy(pgs, p, u);
	        	if(closestEnemy != null)
	        	{
	        		// Check if the unit can go towards the enemy
		        	boolean doesPathExists = DoesPathExists(closestEnemy, u, gs);
	        		System.out.println(doesPathExists);
	        		// If can - attack
	        		if (doesPathExists == true)
	        		{
	        			attack(u, closestEnemy);
	        		}
	        		// Otherwise - stack together
	        		else
	        		{
	        			Stack(gs, attackers);
	        		}
        		}
    		}
    	}
        
    	// Attack with all workers if the enemy is close enough - basically defensive counter-attack
        // Check if we have at least one barracks and enough resources to train something
    	if (nBarracks > 0 && p.getResources() >= 2)
    	{
    		// Loop through every worker
	        for (Unit u : workers)
	        {
	        	// Get the closest enemy
	        	Unit closestEnemy = GetClosestEnemy(pgs, p, u);
	        	boolean doesPathExists = DoesPathExists(closestEnemy, u, gs);
	        	int distance = Math.abs(closestEnemy.getX() - u.getX()) + Math.abs(closestEnemy.getY() - u.getY());
	        	// If true and the close enough - attack!
	        	if(doesPathExists)
	        	{
		        	if (distance < 5)
		        	{
		        		attack(u, closestEnemy);
		        	}
	        	}
	        }
    	}
    }
    
    /**
     * Moves units to their positions stacking together if there is no path to attack the unit
     * @param gs Game State
     * @param units List of units passed in
     */
    public void Stack(GameState gs, List<Unit> units)
    {
    	// Get the physical game state
    	PhysicalGameState pgs = gs.getPhysicalGameState();
    	// If we are on top side - stack my units at the bottom
    	if(isOnTop)
    	{
    		for(int i = 1; i <= units.size(); i++)
    		{
    			if(units.get(i-1).getType() == archer)
    			{
    				move(units.get(i-1), 3, pgs.getHeight() - i);
    			}
    			else if (units.get(i-1).getType() == worker)
    			{
    				move(units.get(i-1), 1, pgs.getHeight() - i);
    			}
    			else if (units.get(i-1).getType() == light || units.get(i-1).getType() == heavy)
    			{
    				move(units.get(i-1), 2, pgs.getHeight() - i);
    			}
    		}
    	}
    	// If we are on the bottom side - stack my units at the top
    	else
    	{
    		for(int i = 1; i <= units.size(); i++)
    		{
    			if(units.get(i-1).getType() == archer)
    			{
    				move(units.get(i-1), i - 1, pgs.getWidth() - 3);
    			}
    			else if (units.get(i-1).getType() == worker)
    			{
    				move(units.get(i-1), i - 1, pgs.getWidth() - 1);
    			}
    			else if (units.get(i-1).getType() == light || units.get(i-1).getType() == heavy)
    			{
    				move(units.get(i-1), i - 1, pgs.getWidth() - 2);
    			}
    		}
    	}
    }
    
    /**
     * Check whether my unit can go towards the closest enemy
     * @param closestEnemy enemy's closest unit
     * @param u My Unit
     * @param gs Game State
     * @return Boolean - True if can go towards the enemy, false if cannot
     */
    public boolean DoesPathExists(Unit closestEnemy, Unit u, GameState gs)
    {
    	boolean doesPathExists = false;
    	if(pf.pathExists(u, (closestEnemy.getY())*(closestEnemy.getX() + 1), gs, null) || 
    			pf.pathExists(u, (closestEnemy.getY())*(closestEnemy.getX() - 1), gs, null) || 
    			pf.pathExists(u, (closestEnemy.getY() + 1)*(closestEnemy.getX()), gs, null) || 
    			pf.pathExists(u, (closestEnemy.getY() - 1)*(closestEnemy.getX()), gs, null))
    	{
    		doesPathExists = true;
    	}
    	return doesPathExists;
    }
    
    /**
     * A function to multiply unit count to be produced depending on the map size
     * @param pgs Physical Game State
     * @return Return integer, which increments depending on the map size.
     */
    public int mapMultiplier(PhysicalGameState pgs)
    {
    	int multiplier = 0;
    	
    	// Increment the multiplier depending on the map size
    	for (int height = 8; height < pgs.getHeight(); height += 8)
    	{
    		multiplier++;
    	}
    	
    	return multiplier;
    }
    
    @Override
    public List<ParameterSpecification> getParameters() 
    {
        List<ParameterSpecification> parameters = new ArrayList<>();
        
        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));
        
        return parameters;
    }
    
}