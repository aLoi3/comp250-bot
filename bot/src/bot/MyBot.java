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
    
    public MyBot(UnitTypeTable utt) {
        super(new AStarPathFinding());
        this.utt = utt;
        worker = utt.getUnitType("Worker");
        archer = utt.getUnitType("Archer");
        light = utt.getUnitType("Light");
        heavy = utt.getUnitType("Heavy");
        barracks = utt.getUnitType("Barracks");
        base = utt.getUnitType("Base");
    }
    

    @Override
    public void reset() {
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
        	
        	// Barracks
        	if(unit.getType() == barracks 
        			&& unit.getPlayer() == player 
        			&& gs.getActionAssignment(unit) == null)
        	{
        		BarracksBehavior(unit, p, pgs);
        	}
        	
        	// Melee
        	if(unit.getType() == light || unit.getType() == heavy 
        			&& unit.getPlayer() == player 
        			&& gs.getActionAssignment(unit) == null)
        	{
        		MeleeBehavior(unit, p, gs);
        	}
        	
        	// Ranged
        	if(unit.getType() == archer 
        			&& unit.getPlayer() == player 
        			&& gs.getActionAssignment(unit) == null)
        	{
        		RangedBehavior(unit, p, gs);
        	}
        	
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
    	for(Unit u2 : pgs.getUnits())
    	{
    		if(u2.getType() == worker && u2.getPlayer() == p.getID())
    		{
    			nWorkers++;
    		}
    	}
    	
    	// Train up to 3 workers
    	if(nWorkers < 3 && p.getResources() >= worker.cost)
    	{
    		train(u, worker);
    	}
    }
    
    // Barracks behavior
    public void BarracksBehavior(Unit u, Player p, PhysicalGameState pgs)
    {
    	int enemyWorkerCount = EnemyUnitCount(p, pgs, worker, archer);
    	if(enemyWorkerCount >= 3)
    	{
    		if (p.getResources() >= archer.cost)
        	{
        		train(u, archer);
        	}
    	}
    	
    	int enemyHeavyCount = EnemyUnitCount(p, pgs, heavy, archer);
    	if(enemyHeavyCount >= 1)
    	{
    		if (p.getResources() >= archer.cost)
	    	{
	    		train(u, archer);
	    	}
    	}
    	
    	int enemyArcherCount = EnemyUnitCount(p, pgs, archer, light);
    	if(enemyArcherCount >= 2)
    	{
    		if (p.getResources() >= light.cost)
	    	{
	    		train(u, light);
	    	}
    	}
    	
    	int enemyLightCount = EnemyUnitCount(p, pgs, light, heavy);
    	if(enemyLightCount >= 2)
    	{
    		if (p.getResources() >= heavy.cost)
	    	{
	    		train(u, heavy);
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
    	
    	int resourcesUsed = 0;
    	
    	List<Unit> freeWorkers = new LinkedList<Unit>();
    	freeWorkers.addAll(workers);
    	
    	if (workers.isEmpty())
    	{
    		return;
    	}
    	
    	for (Unit u1 : pgs.getUnits())
    	{    		
    		if(u1.getType() == base && u1.getPlayer() == p.getID())
    		{
    			nBases++;
    		}
    		
    		if(u1.getType() == barracks && u1.getPlayer() == p.getID())
    		{
    			nBarracks++;
    		}
    	}
    	
    	List<Integer> reservedPositions = new LinkedList<Integer>();
    	if (nBases == 0 && !freeWorkers.isEmpty())
    	{
    		if(p.getResources() >= base.cost + resourcesUsed)
    		{
    			Unit u = freeWorkers.remove(0);
    			buildIfNotAlreadyBuilding(u, base, u.getX(), u.getY(), reservedPositions, p, pgs);
    			resourcesUsed += base.cost;
    		}	
    	}
    	if (nBarracks == 0 && !freeWorkers.isEmpty())
    	{
    		if(p.getResources() >= barracks.cost + resourcesUsed)
    		{
    			Unit u = freeWorkers.remove(0);
    			buildIfNotAlreadyBuilding(u, base, u.getX(), u.getY(), reservedPositions, p, pgs);
    			resourcesUsed += barracks.cost;
    		}
    	}
    	
    	// harvest with all the free workers:
        for (Unit u : freeWorkers) {
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
        	Unit closestEnemy = GetClosestEnemy(pgs, p ,u);
        	int distance = Math.abs(closestEnemy.getX() - u.getX()) + Math.abs(closestEnemy.getY() - u.getY());
        	if (u.getHarvestAmount() == 0)
        	{
        		if (distance < 5)
        		{
        			attack(u, closestEnemy);
        		}
        	}
        }
    }
    
    @Override
    public List<ParameterSpecification> getParameters() 
    {
        List<ParameterSpecification> parameters = new ArrayList<>();
        
        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));
        
        return parameters;
    }
    
}
