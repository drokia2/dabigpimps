//package org.ggp.base.player.gamer.statemachine.sample;
//
//import java.util.List;
//
//import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
//import org.ggp.base.util.statemachine.MachineState;
//import org.ggp.base.util.statemachine.Move;
//import org.ggp.base.util.statemachine.Role;
//import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
//import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
//import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
//
//public class BDSPimp extends SampleGamer {
//
//	@Override
//	public Move stateMachineSelectMove(long timeout)
//			throws TransitionDefinitionException, MoveDefinitionException,
//			GoalDefinitionException {
//		// We get the current start time
//		long start = System.currentTimeMillis();
//
//		/**
//		 * We put in memory the list of legal moves from the
//		 * current state. The goal of every stateMachineSelectMove()
//		 * is to return one of these moves. The choice of which
//		 * Move to play is the goal of GGP.
//		 */
//		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
//
//		// SampleLegalGamer is very simple : it picks the last legal move
//		Move selection = moves.get(moves.size()-1);
//
//		// We get the end time
//		// It is mandatory that stop<timeout
//		long stop = System.currentTimeMillis();
//
//		/**
//		 * These are functions used by other parts of the GGP codebase
//		 * You shouldn't worry about them, just make sure that you have
//		 * moves, selection, stop and start defined in the same way as
//		 * this example, and copy-paste these two lines in your player
//		 */
//		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
//		return selection;
//	}
//
//	Move bestMove(Role role, MachineState state) throws TransitionDefinitionException, MoveDefinitionException,
//			GoalDefinitionException{
//		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
//		Move action = moves.get(0);
//		int score = 0;
//		for (int i = 0; i < moves.length; i++){
//			int result = minscore(role, )
//		}
//		return action;
//	}
//
//}
