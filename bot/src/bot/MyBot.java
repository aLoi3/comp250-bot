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
    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        
        List<Unit> workers = new LinkedList<Unit>();
        for (Unit unit : pgs.getUnits()) 
        {
        	// Base
        	if(unit.getType() == base 
        			&& unit.getPlayer() == player 
        			&& gs.getActionAssignment(unit) == null)
        	{
        		BaseBehavior(unit, p, pgs);
        	}
        }
        	
        for (Unit unit : pgs.getUnits()) 
        {
        	// Barracks
        	if(unit.getType() == barracks 
        			&& unit.getPlayer() == player 
        			&& gs.getActionAssignment(unit) == null)
        	{
        		BarracksBehavior(unit, p, pgs);
        	}
        }
        	
        for (Unit unit : pgs.getUnits()) 
        {
        	// Melee
        	if(unit.getType() == light || unit.getType() == heavy 
        			&& unit.getPlayer() == player 
        			&& gs.getActionAssignment(unit) == null)
        	{
        		MeleeBehavior(unit, p, gs);
        	}
        }
        	
        for (Unit unit : pgs.getUnits()) 
        {
        	// Ranged
        	if(unit.getType() == archer 
        			&& unit.getPlayer() == player 
        			&& gs.getActionAssignment(unit) == null)
        	{
        		RangedBehavior(unit, p, gs);
        	}
        }
        	
        for (Unit unit : pgs.getUnits()) 
        {
        	// Workers
        	if(unit.getPlayer() == player && unit.getType().canHarvest)
        	{
        		workers.add(unit);
        	}
        }
        WorkerBehavior(workers, p, pgs);
        
        return translateActions(player, gs);
    }
    
    // Get the closest enemy
    public Unit GetClosestEnemy(PhysicalGameState pgs, Player p, Unit u)
    {
    	Unit closestEnemy = null;
    	int closestDistance = 0;
    	for(Unit u2:pgs.getUnits())
    	{
    		if(u2.getPlayer() >= 0 && u2.getPlayer() != p.getID())
    		{
    			int distance = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
    			if(closestEnemy == null || distance < closestDistance)
    			{
    				closestEnemy = u2;
    				closestDistance = distance;
    			}
    		}
    	}
    	return closestEnemy;
    }
    
    // Base behavior
    public void BaseBehavior(Unit u, Player p, PhysicalGameState pgs)
    {
    	int nWorkers = 0;
    	int nBarracks = 0;
    	int workerCount = 3;
    	int multiplier = mapMultiplier(pgs);
    	
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
    	
    	if (multiplier > 0)
    	{
    		workerCount = workerCount + (2 * multiplier);
    	}
    	
    	// Train up to 3 workers
    	if(nWorkers < workerCount && p.getResources() >= worker.cost)
    	{
    		if(nWorkers < 2 || nBarracks >= 1 || p.getResources() >= 5)
    		{
    			train(u, worker);
    		}
    	}
    }
    
    // Barracks behavior
    public void BarracksBehavior(Unit u, Player p, PhysicalGameState pgs)
    {
    	TrainTroops(archer, worker, u, p, pgs, 3);
    	TrainTroops(archer, heavy, u, p, pgs, 1);
    	TrainTroops(light, archer, u, p, pgs, 2);
    	TrainTroops(heavy, light, u, p, pgs, 2);
    }
    
    public void TrainTroops(UnitType troopToTrain, UnitType enemyTroop, Unit u, Player p, PhysicalGameState pgs, int enemyAmount)
    {
    	int enemyTroopCount = EnemyUnitCount(p, pgs, enemyTroop, troopToTrain);
    	if (enemyTroopCount >= enemyAmount)
    	{
    		if(p.getResources() >= troopToTrain.cost)
    		{
    			train(u, troopToTrain);
    		}
    	}
    }
    
    public int EnemyUnitCount(Player p, PhysicalGameState pgs, UnitType enemy, UnitType ally)
    {
    	int myUnitCount = 0;
    	int enemyUnitCount = 0;
    	
    	for(Unit u2 : pgs.getUnits())
    	{
    		if(u2.getPlayer() == p.getID() && u2.getType() == ally)
    		{
    			myUnitCount++; 
    		}
    		
    		if(u2.getPlayer() != p.getID() && u2.getType() == enemy)
    		{
    			enemyUnitCount++;
    		}
    	}
    	
    	// ally - archer
    	if(myUnitCount > 0 && enemy == worker)
    	{
    		enemyUnitCount = enemyUnitCount - (myUnitCount * 3);
    	}
    	
    	// ally - heavy
    	if(myUnitCount > 0 && enemy == light)
    	{
    		enemyUnitCount = enemyUnitCount - (myUnitCount * 2);
    	}
    	
    	// ally - archer
    	if(myUnitCount > 0 && enemy == heavy)
    	{
    		enemyUnitCount = enemyUnitCount - (myUnitCount * 1);
    	}
    	
    	// ally - light
    	if(myUnitCount > 0 && enemy == archer)
    	{
    		enemyUnitCount = enemyUnitCount - (myUnitCount * 2);
    	}
    	
    	return enemyUnitCount;
    }
    
    // Melee unit behavior
    public void MeleeBehavior(Unit u, Player p, GameState gs)
    {
    	PhysicalGameState pgs = gs.getPhysicalGameState();
    	Unit closestEnemy = GetClosestEnemy(pgs, p, u);
    	if(closestEnemy != null)
    	{
    		// Light Unit attack behavior
    		if (u.getType() == light)
    		{
    			if(closestEnemy.getType() != heavy)
    			{
    				attack(u, closestEnemy);
    			}
    			else
    			{
    				int distanceX = Math.abs(u.getX() - closestEnemy.getX());
            		int distanceY = Math.abs(u.getY() - closestEnemy.getY());
            		move(u, distanceX, distanceY);
    			}
    		}
    		else if (u.getType() == heavy)
    		{
    			if(closestEnemy.getType() != archer)
    			{
    				attack(u, closestEnemy);
    			}
    			else
    			{
    				int distanceX = Math.abs(u.getX() - closestEnemy.getX());
            		int distanceY = Math.abs(u.getY() - closestEnemy.getY());
            		move(u, distanceX, distanceY);
    			}
    		}
    	}
    }
    
    // Ranged unit behavior
    public void RangedBehavior(Unit u, Player p, GameState gs)
    {
    	PhysicalGameState pgs = gs.getPhysicalGameState();
    	Unit closestEnemy = GetClosestEnemy(pgs, p, u);
    	if (closestEnemy != null)
    	{
    		if(closestEnemy.getType() != light)
    		{
    			// Attack the closes unit if it's not a Heavy
    			attack(u, closestEnemy);
    		}
    		else
        	{
        		// Hopefully move from Heavy units
        		int distanceX = Math.abs(u.getX() - closestEnemy.getX());
        		int distanceY = Math.abs(u.getY() - closestEnemy.getY());
        		move(u, distanceX, distanceY);
        	}
    	}
    }
    
    // Worker behavior ? 
    public void WorkerBehavior(List<Unit> workers, Player p, PhysicalGameState pgs)
    {
    	int nBases = 0;
    	int nBarracks = 0;
    	Unit myBase = null;
    	
    	int resourcesUsed = 0;
    	int resourceCount = 0;
    	int basesToBuild = 1;
    	int barracksToBuild = 1;
    	int harvestersNeeded = 2;
    	
    	int multiplier = mapMultiplier(pgs);

    	List<Unit> allWorkers = new LinkedList<Unit>();
    	allWorkers.addAll(workers);
    	
    	if (workers.isEmpty())
    	{
    		return;
    	}
    	List<Unit> harvesters = new LinkedList<Unit>();
    	for (Unit u2 : allWorkers)
    	{
	    	if (harvesters.size() < harvestersNeeded)
	    	{
	    		harvesters.add(u2);
	    	}
    	}
    	
    //	for (Unit u2 : pgs.getUnits())
    //	{
    //		if(u2.getPlayer() == -1)
    //		{
    //			resourceCount++;
    //		}
    //	}
    //	resourceCount = Math.round(resourceCount / 2);
    	
    	//if(harvesters.size() < 2)
    	//{
    	//	harvesters.add(allWorkers.get(0));
    	//	allWorkers.remove(0);
    	//}
    	
    	for (Unit u2 : pgs.getUnits())
    	{    		
    		if(u2.getType() == base && u2.getPlayer() == p.getID())
    		{
    			nBases++;
    			myBase = u2;
    		}
    		
    		if(u2.getType() == barracks && u2.getPlayer() == p.getID())
    		{
    			nBarracks++;
    		}
    	}
    	
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
    	
    	List<Integer> reservedPositions = new LinkedList<Integer>();
    	// Build Bases
    	if (nBases < basesToBuild && !allWorkers.isEmpty())
    	{
    		if(p.getResources() >= base.cost)
    		{
    			Unit u = allWorkers.remove(allWorkers.size() - 1);
    			buildIfNotAlreadyBuilding(u, base, u.getX(), u.getY(), reservedPositions, p, pgs);
    			//resourcesUsed += base.cost;
    		}	
    	}
    	// Build Barracks
    	if (nBarracks < (barracksToBuild + multiplier) && !allWorkers.isEmpty())
    	{
    		if(p.getResources() >= barracks.cost)
    		{
				Unit u = allWorkers.remove(allWorkers.size() - 1);
			
    			if(isOnTop)
        		{
    				if(nBarracks == 0)
    				{
    					buildIfNotAlreadyBuilding(u, barracks, myBase.getX() + 3, myBase.getY() + 1, reservedPositions, p, pgs);
    				}
    				if (nBarracks == 1)
    				{
    					buildIfNotAlreadyBuilding(u, barracks, myBase.getX() + 1, myBase.getY() + 3, reservedPositions, p, pgs);
    				}
    				else
    				{
    					buildIfNotAlreadyBuilding(u, barracks, u.getX(), u.getY(), reservedPositions, p, pgs);
    				}
        		}
    			else
    			{
    				if(nBarracks == 0)
    				{
    					buildIfNotAlreadyBuilding(u, barracks, myBase.getX() - 3, myBase.getY() - 1, reservedPositions, p, pgs);
    				}
    				if (nBarracks == 1)
    				{
    					buildIfNotAlreadyBuilding(u, barracks, myBase.getX() - 1, myBase.getY() - 3, reservedPositions, p, pgs);
    				}
    				else
    				{
    					buildIfNotAlreadyBuilding(u, barracks, u.getX(), u.getY(), reservedPositions, p, pgs);
    				}
    			}
    			//resourcesUsed += barracks.cost;
    		}
    	}
    	
    	// harvest with all the free workers:
        for (Unit u : harvesters) {
            Unit closestBase = null;
            Unit closestResource = null;
            int closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) 
            {
                if (u2.getType().isResource) 
                {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestResource == null || d < closestDistance) 
                    {
                        closestResource = u2;
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
        
        // Command workers to attack if the enemy is close to them (Hopefully)
        for (Unit u : workers)
        {
        	// Attack if enemy unit is close to a worker
        	Unit closestEnemy = GetClosestEnemy(pgs, p ,u);
        	int distance = Math.abs(closestEnemy.getX() - u.getX()) + Math.abs(closestEnemy.getY() - u.getY());
        	if (distance < 5)
        	{
        		attack(u, closestEnemy);
        	}
        }
    }
    
    public int mapMultiplier(PhysicalGameState pgs)
    {
    	int multiplier = 0;
    	
    	for (int height=8; height<pgs.getHeight(); height+=8)
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
