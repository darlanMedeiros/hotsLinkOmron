export type Fabrica = {
  id: number;
  name: string;
};

export type MiniFabrica = {
  id: number;
  name: string;
  fabricaId: number;
  setorIds: number[];
};

export type Setor = {
  id: number;
  name: string;
};

export type Machine = {
  id: number;
  name: string;
  deviceId: number;
  miniFabricaId: number;
  setorId: number;
};

export type Device = {
  id: number;
  mnemonic: string;
  name: string;
};

export type Turno = {
  id: number;
  name: string;
  horaInicio: string;
  horaFinal: string;
};

export type ViewMessage = {
  type: 'success' | 'error';
  text: string;
};

export interface SectionSharedProps {
  isSaving: boolean;
  withMutation: (work: () => Promise<void>, successText: string) => Promise<void>;
  removeItem: (url: string, successText: string) => Promise<void>;
}
