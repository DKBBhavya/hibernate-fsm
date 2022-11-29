package com.skydo.lib.fsm.listener;

import com.skydo.lib.fsm.config.StateValidatorConfig;
import com.skydo.lib.fsm.internal.tools.Pair;
import com.skydo.lib.fsm.servicecontributor.FSMService;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Raj Sheth
 */

public class FSMPostCommitInsertListener extends BaseEventListener implements PostCommitInsertEventListener {


	private final static Logger log = LoggerFactory.getLogger(FSMPostCommitUpdateListener.class);

	public FSMPostCommitInsertListener(FSMService fsmService) {
		super(fsmService);
	}

	private boolean onPostCommit(PostInsertEvent postInsertEvent) {

		StateValidatorConfig stateValidatorConfig = getFsmService().getStateValidator();
		HashMap<Class<?>, HashMap<String, HashMap<String, Pair<Object, List<Method>>>>> entityFieldPostUpdateActionMap
				= stateValidatorConfig.getEntityFieldPostUpdateActionMap();

		Object entity = postInsertEvent.getEntity();

		Class<? extends Object> entityClass = entity.getClass();
		HashMap<String, HashMap<String, Pair<Object, List<Method>>>> fieldToValuesMap = entityFieldPostUpdateActionMap.get(entityClass);

		if (entityFieldPostUpdateActionMap.containsKey(entityClass)) {
			String[] propertyNames = postInsertEvent.getPersister().getPropertyNames();
			List newValues = Arrays.stream(postInsertEvent.getState()).toList();
			int totalFields = newValues.size();

			for(int i = 0 ; i < totalFields ; ++i) {
				String propertyName = propertyNames[i];
				Object currentNewValue = null;
				if (newValues.get(i) != null) {
					currentNewValue = newValues.get(i).toString();
				}

				if (fieldToValuesMap.containsKey(propertyName)) {
					HashMap<String, Pair<Object, List<Method>>> valuesToPostActionMethods = fieldToValuesMap.get(propertyName);

					if (valuesToPostActionMethods.containsKey(currentNewValue)) {
						Pair<Object, List<Method>> postActionMethodPair = valuesToPostActionMethods.get(currentNewValue);
						if (postActionMethodPair != null && postActionMethodPair.getSecond() != null) {
							Object finalCurrentNewValue = currentNewValue;
							postActionMethodPair.getSecond().forEach(method -> {
								try {
									method.invoke(
										postActionMethodPair.getFirst(),
										postInsertEvent.getId(),
										// TODO: can we handle this better?
										//  Old value is empty string
										"",
										finalCurrentNewValue
									);
								} catch (IllegalAccessException | InvocationTargetException e) {
									throw new RuntimeException(e);
								}
							});
						}
					}
				}
			}
		}
		return true;
	}


	/**
	 * Called when a commit fails and an entity was scheduled for insertion
	 *
	 * @param event the insert event to be handled
	 */
	@Override
	public void onPostInsertCommitFailed(PostInsertEvent event) {

	}

	@Override
	public void onPostInsert(PostInsertEvent event) {
		onPostCommit(event);
	}

	/**
	 * Does this listener require that after transaction hooks be registered?
	 *
	 * @param persister The persister for the entity in question.
	 * @return {@code true} if after transaction callbacks should be added.
	 * @deprecated use {@link #requiresPostCommitHandling(EntityPersister)}
	 */
	@Override
	public boolean requiresPostCommitHanding(EntityPersister persister) {
		return true;
	}
}
