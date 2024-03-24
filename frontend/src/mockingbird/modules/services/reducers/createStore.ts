import { createReducer, createEvent } from '@tramvai/state';
import i18n from 'src/mockingbird/i18n';

export type ServiceState = {
  status: 'none' | 'loading' | 'complete' | 'error';
  id?: string;
  errorMessage?: string;
};

const storeName = 'createServiceState';
const initialState: ServiceState = {
  status: 'none',
};

export const createSuccess = createEvent<string>('CREATE_SERVICE_SUCCESS');
export const createFail = createEvent<any>('CREATE_SERVICE_FAIL');
export const setLoading = createEvent('SET_LOADING_CREATE_SERVICE');
export const reset = createEvent('RESET_CREATE_SERVICE');

const reducer = createReducer(storeName, initialState)
  .on(createSuccess, (state, id) => ({
    id,
    status: 'complete',
  }))
  .on(createFail, (state, e) => ({
    status: 'error',
    errorMessage: (e && e.body && e.body.error) || i18n.t('services.tryAgain'),
  }))
  .on(setLoading, () => ({
    status: 'loading',
  }));

reducer.on(reset, () => initialState);

export default reducer;
