export class RepositoriesDTO {

  constructor(data:Partial<RepositoriesDTO>) {
    Object.assign(this, data);
  }

  id?: number|null;
  repoName?: string|null;
  repoCreatedDate?: string|null;

}
