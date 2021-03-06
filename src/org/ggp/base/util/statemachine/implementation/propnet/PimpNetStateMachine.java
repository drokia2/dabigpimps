package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.PropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;


@SuppressWarnings("unused")
public class PimpNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;

    private MachineState initial;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
        propNet = PropNetFactory.create(description);
        roles = propNet.getRoles();
        System.out.println("Getting ordering!");
        ordering = getOrdering();
        System.out.println("Got ordered!");
        initial = computeInitialState();
    }

    private MachineState computeInitialState() {
		// TODO Auto-generated method stub
    	for (Proposition baseProp : propNet.getBasePropositions().values()){
    		baseProp.setValue(false);
    	}
    	propNet.getInitProposition().setValue(true);
    	propagate();
    	MachineState initialState = getStateFromBase();
    	propNet.getInitProposition().setValue(false);
		return initialState;
	}

	private boolean markBases(MachineState state){
    	Map<GdlSentence, Proposition> baseProps = propNet.getBasePropositions();
    	Set<GdlSentence> currSentences = state.getContents();
    	for (Proposition baseProp : baseProps.values()){
    		baseProp.setValue(false);
    	}
    	for (GdlSentence sentence: currSentences){
    		Proposition prop = baseProps.get(sentence.toTerm());
    		if (prop != null) prop.setValue(true);
    	}
    	return true;
    }

    private boolean markActions(List<Move> moves){
    	Map<GdlSentence, Proposition> inputProps = propNet.getInputPropositions();
    	for (Proposition inputProp : inputProps.values()){
    		inputProp.setValue(false);
    	}
    	List<GdlSentence> moveTerms = toDoes(moves);
    	for (GdlSentence sentence: moveTerms){
    		Proposition prop = inputProps.get(sentence.toTerm());
    		if (prop != null) prop.setValue(true);;
    	}
    	return true;
    }

    private void propagate(){
    	for (Proposition prop : ordering) prop.setValue(prop.getSingleInput().getValue());
    }

	/**
	 * Computes if the state is terminal. Should return the value
	 * of the terminal proposition for the state.
	 */
	@Override
	public boolean isTerminal(MachineState state) {
		// TODO: Compute whether the MachineState is terminal.
		markBases(state);
		propagate();
		return propNet.getTerminalProposition().getValue();
	}

	/**
	 * Computes the goal for a role in the current state.
	 * Should return the value of the goal proposition that
	 * is true for that role. If there is not exactly one goal
	 * proposition true for that role, then you should throw a
	 * GoalDefinitionException because the goal is ill-defined.
	 */
	@Override
	public int getGoal(MachineState state, Role role)
	throws GoalDefinitionException {
		// TODO: Compute the goal for role in state.
		markBases(state);
		propagate();
		Set<Proposition> props = propNet.getGoalPropositions().get(role);
		Proposition goalProp = null;
		int numTrueProps = 0;
		for (Proposition prop : props){
			if(prop.getValue()){
				if (numTrueProps == 0){
					numTrueProps++;
					goalProp = prop;
				} else {
					throw new GoalDefinitionException(state, role);
				}
			}
		}
		return getGoalValue(goalProp);
	}

	/**
	 * Returns the initial state. The initial state can be computed
	 * by only setting the truth value of the INIT proposition to true,
	 * and then computing the resulting state.
	 */
	@Override
	public MachineState getInitialState() {
		// TODO: Compute the initial state.
		return initial;
	}

	/**
	 * Computes the legal moves for role in state.
	 */
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
	throws MoveDefinitionException {
		// TODO: Compute legal moves.
		markBases(state);
		propagate();
		Set<Proposition> legalProps = propNet.getLegalPropositions().get(role);
		List<Move> legalMoveList = new ArrayList<Move>();
		for (Proposition legalProp : legalProps){
			if (legalProp.getValue()){
				Move legalMove = getMoveFromProposition(legalProp);
				legalMoveList.add(legalMove);
			}
		}
		return legalMoveList;
	}

	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
	throws TransitionDefinitionException {
		// TODO: Compute the next state.
		markBases(state);
		markActions(moves);
		propagate();
		Set<GdlSentence> nextStateSentences = new HashSet<GdlSentence>();
		for (GdlSentence sentence : propNet.getBasePropositions().keySet()){
			Proposition prop = propNet.getBasePropositions().get(sentence);
			if (prop.getSingleInput().getValue()){
				nextStateSentences.add(prop.getName());
			}
		}
		return new MachineState(nextStateSentences);
	}

	/**
	 * This should compute the topological ordering of propositions.
	 * Each component is either a proposition, logical gate, or transition.
	 * Logical gates and transitions only have propositions as inputs.
	 *
	 * The base propositions and input propositions should always be exempt
	 * from this ordering.
	 *
	 * The base propositions values are set from the MachineState that
	 * operations are performed on and the input propositions are set from
	 * the Moves that operations are performed on as well (if any).
	 *
	 * @return The order in which the truth values of propositions need to be set.
	 */
	public List<Proposition> getOrdering()
	{
	    // List to contain the topological ordering.
	    List<Proposition> order = new LinkedList<Proposition>();
	    List<Component> compOrder = new LinkedList<Component>();

		// All of the components in the PropNet
		List<Component> components = new ArrayList<Component>(propNet.getComponents());

		// All of the propositions in the PropNet.
		List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

		List<Proposition> basePropositions = new ArrayList<Proposition>(propNet.getBasePropositions().values());
		List<Proposition> inputPropositions = new ArrayList<Proposition>(propNet.getInputPropositions().values());
		List<Proposition> independentPropositions = new ArrayList<Proposition>();
		independentPropositions.add(propNet.getInitProposition());
		independentPropositions.addAll(inputPropositions);
		independentPropositions.addAll(basePropositions);
		//int count = 0;
		System.out.println("Propositions' Size: " + (propositions.size() - independentPropositions.size()));
		List<Proposition> depPropositions = new ArrayList<Proposition>(propositions);
		depPropositions.removeAll(independentPropositions);
		while(true) {
			System.out.println("Ordering size: " + compOrder.size());
			List<Component> toBeAdded = new ArrayList<Component>();
			for(Proposition prop : depPropositions) {
				Set<Component> inputs = prop.getSingleInput().getInputs();
				Set<Component> curOrder = new HashSet<Component>(compOrder);
				curOrder.addAll(independentPropositions);
				if (compOrder.contains(prop)) {
					continue;
				} else if (curOrder.containsAll(inputs)) {
					//count++;
					compOrder.add(prop);
				} else {
					toBeAdded.add(prop);
				}
			}
			if (toBeAdded.isEmpty()) break;
			components = toBeAdded;

		}
		System.out.println("Ordering size: " + compOrder.size());


		for (Component comp : compOrder) {
			if (comp instanceof Proposition ) {
				order.add((Proposition)comp);
			}
		}
		//System.out.println()
//		while (!propositions.isEmpty()){
//			Proposition curr = propositions.remove(0);
//			if(curr.getInputs().size()==0) continue;
//			if(basePropositions.contains(curr)) continue;
//			if(inputPropositions.contains(curr)) continue;
//			if(propNet.getInitProposition().equals(curr)) continue;
//
//			Set<Component> inputs = curr.getSingleInput().getInputs();
//			boolean allInputsAdded = true;
//
//			for (Component currComp : inputs){
//				if (!order.contains(currComp)){
//					allInputsAdded = false;
//					break;
//				}
//				if(((Proposition)currComp).getInputs().size()==0 ||
//						basePropositions.contains((Proposition)currComp) ||
//						inputPropositions.contains((Proposition)currComp) ||
//						propNet.getInitProposition().equals(currComp)){
//					allInputsAdded = false;
//					break;
//				}
//			}
//
//			if (allInputsAdded) order.add(curr);
//			else propositions.add(curr);
//		}

		return order;
	}

	/* Already implemented for you */
	@Override
	public List<Role> getRoles() {
		return roles;
	}

	/* Helper methods */

	/**
	 * The Input propositions are indexed by (does ?player ?action).
	 *
	 * This translates a list of Moves (backed by a sentence that is simply ?action)
	 * into GdlSentences that can be used to get Propositions from inputPropositions.
	 * and accordingly set their values etc.  This is a naive implementation when coupled with
	 * setting input values, feel free to change this for a more efficient implementation.
	 *
	 * @param moves
	 * @return
	 */
	private List<GdlSentence> toDoes(List<Move> moves)
	{
		List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();

		for (int i = 0; i < roles.size(); i++)
		{
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
		}
		return doeses;
	}

	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding Move
	 * @param p
	 * @return a PropNetMove
	 */
	public static Move getMoveFromProposition(Proposition p)
	{
		return new Move(p.getName().get(1));
	}

	/**
	 * Helper method for parsing the value of a goal proposition
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */
    private int getGoalValue(Proposition goalProposition)
	{
		GdlRelation relation = (GdlRelation) goalProposition.getName();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}

	/**
	 * A Naive implementation that computes a PropNetMachineState
	 * from the true BasePropositions.  This is correct but slower than more advanced implementations
	 * You need not use this method!
	 * @return PropNetMachineState
	 */
	public MachineState getStateFromBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : propNet.getBasePropositions().values())
		{
			p.setValue(p.getSingleInput().getValue());
			if (p.getValue())
			{
				contents.add(p.getName());
			}

		}
		return new MachineState(contents);
	}
}