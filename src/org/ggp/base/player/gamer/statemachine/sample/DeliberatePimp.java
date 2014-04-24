package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/**
 * SampleLegalGamer is a minimal gamer which always plays the first
 * legal move it identifies, regardless of the state of the game.
 *
 * For your first players, you should extend the class SampleGamer
 * The only function that you are required to override is :
 * public Move stateMachineSelectMove(long timeout)
 *
 */
public final class DeliberatePimp extends SampleGamer
{
	/**
	 * This function is called at the start of each round
	 * You are required to return the Move your player will play
	 * before the timeout.
	 *
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// We get the current start time
		long start = System.currentTimeMillis();


		/**
		 * We put in memory the list of legal moves from the
		 * current state. The goal of every stateMachineSelectMove()
		 * is to return one of these moves. The choice of which
		 * Move to play is the goal of GGP.
		 */
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());

		// SampleLegalGamer is very simple : it picks the last legal move
		Move selection = bestMove(getRole(), getCurrentState());

		// We get the end time
		// It is mandatory that stop<timeout
		long stop = System.currentTimeMillis();

		/**
		 * These are functions used by other parts of the GGP codebase
		 * You shouldn't worry about them, just make sure that you have
		 * moves, selection, stop and start defined in the same way as
		 * this example, and copy-paste these two lines in your player
		 */
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	private Move bestMove(Role role, MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		List<Move> legalMoves = getStateMachine().getLegalMoves(state, role);
		int score = 0;
		Move bestAction = legalMoves.get(0);
		for (int i = 0; i < legalMoves.size(); i++) {
			Move move = legalMoves.get(i);
			Move noop = new Move(GdlPool.getConstant("noop"));
			int result = maxScore(role, getStateMachine().getNextState(state, Arrays.asList(new Move[] {move, noop})));
			if (result == 100)  {
				return move;
			}
			if (result > score) {
				score = result;
				bestAction = move;
			}
		}
		return bestAction;
	}

	private int  maxScore(Role role, MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		if (getStateMachine().isTerminal(state))  {
			return getStateMachine().getGoal(state, role);
		}
		List<Move> legalMoves = getStateMachine().getLegalMoves(state, role);
		int score = 0;
		for (int i = 0; i < legalMoves.size(); i++) {
			Move move = legalMoves.get(i);

			int result = maxScore(role, getStateMachine().getNextState(state, new ArrayList<Move>(Arrays.asList(move))));
			if (result > score) {
				score = result;
			}
		}
		return score;

	}
}